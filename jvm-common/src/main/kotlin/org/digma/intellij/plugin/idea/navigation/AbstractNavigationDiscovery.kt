package org.digma.intellij.plugin.idea.navigation

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.concurrency.AppExecutorUtil
import kotlinx.datetime.Clock
import org.digma.intellij.plugin.common.SearchScopeProvider
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.common.isValidVirtualFile
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.navigation.model.NavigationDiscoveryTrigger
import org.digma.intellij.plugin.idea.navigation.model.NavigationProcessContext
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.process.ProcessManager
import org.digma.intellij.plugin.session.SessionMetadataProperties
import org.digma.intellij.plugin.session.getPluginLoadedKey
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.time.Duration

abstract class AbstractNavigationDiscovery(protected val project: Project) : Disposable {

    protected val logger = Logger.getInstance(this::class.java)

    //used to restrict one update thread at a time
    protected val buildLock = ReentrantLock()

    private val scheduledExecutorService: ScheduledExecutorService
        get() = AppExecutorUtil.createBoundedScheduledExecutorService("${this.type}Nav", 1)


    override fun dispose() {
        try {
            scheduledExecutorService.shutdownNow()
        } catch (e: Throwable) {
            //ignore
        }
    }

    abstract val type: String

    abstract fun removeDiscoveryForFile(file: VirtualFile)

    abstract fun removeDiscoveryForPath(path: String)

    abstract fun getTask(myContext: NavigationProcessContext, navigationDiscoveryTrigger: NavigationDiscoveryTrigger, retry: Int): Runnable

    abstract fun getNumFound(): Int

    //navigation discovery will run on startup using a single thread scheduler and invisible retryable task.
    // the task will retry a few times in case of errors but in the meantime will collect discovery objects and store them.
    //there are two levels of retry, the task itself is retryable and will retry every few seconds if the process failed.
    //if failed after all attempts a new task will be scheduled to run after 2 minutes in hope it will complete with no errors.
    //max 20 attempts to complete with no errors.
    //on file changed the same thing will happen for one file.
    fun buildNavigationDiscovery() {
        schedule({ GlobalSearchScope.projectScope(project) }, null, NavigationDiscoveryTrigger.Startup)
    }

    fun buildNavigationDiscoveryFullUpdate() {
        schedule({ GlobalSearchScope.projectScope(project) }, null, NavigationDiscoveryTrigger.FullUpdate)
    }


    protected fun schedule(
        searchScopeProvider: SearchScopeProvider,
        preTask: Runnable?, navigationDiscoveryTrigger: NavigationDiscoveryTrigger, delayMillis: Long = 0, retry: Int = 0,
    ) {

        //this method should be very fast, only schedule a task. it may be called on EDT.

        if (!isProjectValid(project)) {
            return
        }

        Log.log(logger::trace, project, "Scheduling navigation discovery trigger {}, retry {}", navigationDiscoveryTrigger, retry)

        //max 20 retries and give up
        if (retry > 20) {
            Log.log(
                logger::trace,
                project,
                "Max retries exceeded, Not scheduling navigation discovery trigger {}, retry {}",
                navigationDiscoveryTrigger,
                retry
            )
            return
        }

        //scheduledExecutorService is single thread and thus only one thread at a time will process scheduled tasks.
        // which means that scheduling is not accurate and does not need to be in this case.
        scheduledExecutorService.schedule({

            try {
                Log.log(
                    logger::trace,
                    project,
                    "Starting navigation discovery process for {},trigger {},retry {}",
                    type,
                    navigationDiscoveryTrigger,
                    retry
                )
                Log.log(logger::trace, project, "waiting for smart mode for {},trigger {},retry {}", type, navigationDiscoveryTrigger, retry)
                DumbService.getInstance(project).waitForSmartMode()
                Log.log(logger::trace, project, "processing in smart mode for {},trigger {},retry {}", type, navigationDiscoveryTrigger, retry)
                //run the preTask on same thread before the main task
                preTask?.run()
                buildNavigationUnderProgress(searchScopeProvider, navigationDiscoveryTrigger, retry)
            } catch (e: Throwable) {
                Log.warnWithException(logger, project, e, "Error in navigation discovery process {}", e)
                ErrorReporter.getInstance().reportError(project, "${this::class.simpleName}.schedule", e)
            } finally {
                Log.log(
                    logger::trace,
                    project,
                    "Navigation discovery process completed for {},trigger {},retry {}",
                    type,
                    navigationDiscoveryTrigger,
                    retry
                )
            }

        }, delayMillis, TimeUnit.MILLISECONDS)
    }


    private fun buildNavigationUnderProgress(
        searchScopeProvider: SearchScopeProvider,
        navigationDiscoveryTrigger: NavigationDiscoveryTrigger,
        retry: Int
    ) {

        Log.log(logger::trace, project, "Building navigation discovery process for {},trigger {},retry {}", type, navigationDiscoveryTrigger, retry)

        if (!isProjectValid(project)) {
            return
        }

        registerStartEvent("$type-discovery-started", navigationDiscoveryTrigger, retry)

        val processName = "${this::class.simpleName}.buildNavigationUnderProgress"

        val context = NavigationProcessContext(searchScopeProvider, processName)
        val task = getTask(context, navigationDiscoveryTrigger, retry)
        val processResult = project.service<ProcessManager>().runTaskUnderProcess(task, context, true, 10, true)

        registerFinishedEvent(
            "$type-discovery-finished",
            navigationDiscoveryTrigger,
            processResult.success,
            processResult.duration,
            retry,
            context.hasErrors(),
            processResult.error
        )

        //if process failed, schedule another retry immediately.
        //if process succeeded but there are errors schedule again in 2 minutes,hopefully with fewer errors
        if (!processResult.success) {
            Log.log(
                logger::trace,
                project,
                "Navigation discovery process failed for {},trigger {},retry {},processResult {},please check the log",
                type,
                navigationDiscoveryTrigger,
                retry,
                processResult
            )
            schedule(searchScopeProvider, null, navigationDiscoveryTrigger, 0, retry + 1)
        } else if (context.hasErrors()) {
            Log.log(
                logger::trace, project, "Navigation discovery process had errors for {},trigger {},retry {},processResult {},please check the log",
                type,
                navigationDiscoveryTrigger,
                retry,
                processResult
            )
            schedule(searchScopeProvider, null, navigationDiscoveryTrigger, TimeUnit.MINUTES.toMillis(2L), retry + 1)
        } else {
            Log.log(
                logger::trace,
                project,
                "Navigation discovery process completed successfully for {},trigger {},retry {},processResult {}",
                type,
                navigationDiscoveryTrigger,
                retry,
                processResult
            )
        }

        context.logErrors(logger, project, true)

    }

    /*
    This method must be called with a file that is relevant for span discovery and span navigation.
    checking that it is should be done before calling this method.
    */
    fun fileChanged(virtualFile: VirtualFile?) {

        //this method should be very fast and only schedule a task for the changed file.

        if (!isProjectValid(project) || !isValidVirtualFile(virtualFile)) {
            return
        }

        Log.log(logger::trace, project, "got fileChanged for {}, {}", type, virtualFile)

        try {
            virtualFile?.takeIf { isValidVirtualFile(virtualFile) }?.let { vf ->
                //must remove the document spans before building discovery for that file again,
                // the preTask sent to schedule will do it on the same thread where the task will run
                schedule(
                    { GlobalSearchScope.fileScope(project, virtualFile) },
                    { removeDiscoveryForFile(vf) },
                    NavigationDiscoveryTrigger.FileChanged
                )
            }
        } catch (e: Throwable) {
            Log.warnWithException(logger, project, e, "Exception in fileChanged")
            ErrorReporter.getInstance().reportError(project, "${this::class.simpleName}.fileChanged", e)
        }
    }


    fun fileDeleted(virtualFile: VirtualFile?) {

        scheduledExecutorService.schedule({
            if (isProjectValid(project)) {

                if (virtualFile != null) {
                    buildLock.lock()
                    try {
                        removeDiscoveryForFile(virtualFile)
                    } finally {
                        if (buildLock.isHeldByCurrentThread) {
                            buildLock.unlock()
                        }
                    }
                }
            }
        }, 0, TimeUnit.MILLISECONDS)
    }

    fun pathDeleted(path: String) {

        scheduledExecutorService.schedule({
            if (isProjectValid(project)) {

                buildLock.lock()
                try {
                    removeDiscoveryForPath(path)
                } finally {
                    if (buildLock.isHeldByCurrentThread) {
                        buildLock.unlock()
                    }
                }
            }
        }, 0, TimeUnit.MILLISECONDS)
    }


    private fun registerStartEvent(eventName: String, navigationDiscoveryTrigger: NavigationDiscoveryTrigger, retry: Int) {

        //no need to send the event on file changed because it is too many
        if (navigationDiscoveryTrigger == NavigationDiscoveryTrigger.FileChanged) {
            return
        }


        val pluginLoadedTime = SessionMetadataProperties.getInstance().getCreated(getPluginLoadedKey(project))
        val duration = pluginLoadedTime?.let {
            Clock.System.now().minus(it)
        } ?: Duration.ZERO

        val startupLagMillis = duration.inWholeMilliseconds

        val startupLagToShow =
            if (startupLagMillis > 2000) "${TimeUnit.MILLISECONDS.toSeconds(startupLagMillis)} seconds" else "$startupLagMillis millis"

        val details = if (navigationDiscoveryTrigger == NavigationDiscoveryTrigger.Startup) {
            mapOf(
                "startupLag" to startupLagToShow,
                "navigationDiscoveryTrigger" to navigationDiscoveryTrigger,
                "retry" to retry
            )
        } else {
            mapOf(
                "navigationDiscoveryTrigger" to navigationDiscoveryTrigger,
                "retry" to retry
            )
        }

        ActivityMonitor.getInstance(project).registerJvmNavigationDiscoveryEvent(
            eventName,
            details
        )
    }

    private fun registerFinishedEvent(
        eventName: String,
        navigationDiscoveryTrigger: NavigationDiscoveryTrigger,
        success: Boolean,
        duration: Duration,
        retry: Int,
        hadErrors: Boolean,
        error: Throwable?
    ) {

        //no need to send the event on file changed because it is too many
        if (navigationDiscoveryTrigger == NavigationDiscoveryTrigger.FileChanged) {
            return
        }

        ActivityMonitor.getInstance(project).registerJvmNavigationDiscoveryEvent(
            eventName,
            mapOf(
                "duration" to duration.inWholeMilliseconds,
                "found.locations" to getNumFound(),
                "navigationDiscoveryTrigger" to navigationDiscoveryTrigger,
                "success" to success,
                "retry" to retry,
                "hadErrors" to hadErrors,
                "error" to error.toString()
            )
        )
    }

}