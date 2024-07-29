package org.digma.intellij.plugin.idea.navigation

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeAnyChangeAbstractAdapter
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Alarm
import com.intellij.util.concurrency.AppExecutorUtil
import kotlinx.datetime.Clock
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.ReadActions
import org.digma.intellij.plugin.common.SearchScopeProvider
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.common.isValidVirtualFile
import org.digma.intellij.plugin.common.runInReadAccessWithResult
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.navigation.model.NavigationDiscoveryTrigger
import org.digma.intellij.plugin.idea.navigation.model.NavigationProcessContext
import org.digma.intellij.plugin.idea.psi.isJvmSupportedFile
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.process.ProcessManager
import org.digma.intellij.plugin.psi.PsiUtils
import org.digma.intellij.plugin.session.SessionMetadataProperties
import org.digma.intellij.plugin.session.getPluginLoadedKey
import java.util.concurrent.ConcurrentHashMap
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


    init {
        //we need PsiTreeChange because bulk file listener does not always notify on time on changes, it will notify all changes only
        // when saving files. for example if refactoring name for class A that requires changes on class B, bulk file listener will notify on A,
        // but not on B, only when the system decides to save changes it will notify on B.
        //psi change events will be fired immediately on all changed classes.
        //on the other hand psi change events will not be fired for file deletion, but bulk listener will.
        //so we use a combination of both.
        //many times we will be notified for the same file more than once, and we will run a process more than once for the same file, while its better
        // not to the performance and resource consumption is not much more.
        @Suppress("LeakingThis")
        PsiManager.getInstance(project).addPsiTreeChangeListener(MyPsiTreeAnyChangeListener(this), this)
    }


    override fun dispose() {
        scheduledExecutorService.shutdownNow()
    }

    abstract val type: String

    abstract fun removeDiscoveryForFile(file: VirtualFile)

    abstract fun removeDiscoveryForPath(path: String)

    abstract fun getTask(myContext: NavigationProcessContext): Runnable

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


    protected fun schedule(
        searchScopeProvider: SearchScopeProvider,
        preTask: Runnable?, navigationDiscoveryTrigger: NavigationDiscoveryTrigger, delayMillis: Long = 0, retry: Int = 0,
    ) {

        //this method should be very fast, only schedule a task. it may be called on EDT.

        if (!isProjectValid(project)) {
            return
        }

        Log.log(logger::trace, "Scheduling navigation discovery , retry {}", retry)

        //max 20 retries and give up
        if (retry > 20) {
            Log.log(logger::trace, "Max retries exceeded, Not scheduling navigation discovery , retry {}", retry)
            return
        }

        //scheduledExecutorService is single thread and thus only one thread at a time will process scheduled tasks.
        // which means that scheduling is not accurate and does not need to be in this case.
        scheduledExecutorService.schedule({

            try {
                Log.log(logger::trace, "Starting navigation discovery process in smart mode")
                DumbService.getInstance(project).waitForSmartMode()
                //run the preTask on same thread before the main task
                preTask?.run()
                buildNavigationUnderProgress(searchScopeProvider, navigationDiscoveryTrigger, retry)
            } catch (e: Throwable) {
                Log.warnWithException(logger, project, e, "Error in navigation discovery process")
                ErrorReporter.getInstance().reportError(project, "${this::class.simpleName}.schedule", e)
            }

        }, delayMillis, TimeUnit.MILLISECONDS)
    }


    private fun buildNavigationUnderProgress(
        searchScopeProvider: SearchScopeProvider,
        navigationDiscoveryTrigger: NavigationDiscoveryTrigger,
        retry: Int
    ) {

        Log.log(logger::trace, "Building navigation discovery process")

        if (!isProjectValid(project)) {
            return
        }

        registerStartEvent("$type-discovery-started", navigationDiscoveryTrigger, retry)

        val processName = "${this::class.simpleName}.buildNavigationUnderProgress"

        val context = NavigationProcessContext(searchScopeProvider, processName)
        val task = getTask(context)
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
            Log.log(logger::trace, "Navigation discovery process failed,please check the log {}", processResult)
            schedule(searchScopeProvider, null, navigationDiscoveryTrigger, 0, retry + 1)
        } else if (context.hasErrors()) {
            Log.log(logger::trace, "Navigation discovery process had errors,please check the log {}", processResult)
            schedule(searchScopeProvider, null, navigationDiscoveryTrigger, TimeUnit.MINUTES.toMillis(2L), retry + 1)
        } else {
            Log.log(logger::trace, "Navigation discovery process completed successfully {}", processResult)
        }

        context.logErrors(logger, project, true)

    }

    fun documentChanged(document: Document) {

        //this method is called on background thread from documentChanged event so that the EDT is released quickly.

        EDT.assertNonDispatchThread()
        ReadActions.assertNotInReadAccess()

        try {
            if (!isProjectValid(project)) {
                return
            }

            val psiFile = runInReadAccessWithResult {
                PsiDocumentManager.getInstance(project).getPsiFile(document)
            }

            psiFile?.let {
                if (PsiUtils.isValidPsiFile(it) && isJvmSupportedFile(project, it)) {
                    val virtualFile = FileDocumentManager.getInstance().getFile(document)
                    virtualFile?.takeIf { isValidVirtualFile(virtualFile) }?.let { vf ->
                        fileChanged(vf)
                    }
                }
            }
        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "Exception in documentChanged")
            ErrorReporter.getInstance().reportError(project, "${this::class.simpleName}.documentChanged", e)
        }
    }


    /*
    This method must be called with a file that is relevant for span discovery and span navigation.
    checking that should be done before calling this method.
    */
    fun fileChanged(virtualFile: VirtualFile?) {

        //this method should be very fast and only schedule a task for the changed file.
        //it's called on background from BulkFileChangeListenerForJvmNavigationDiscovery.processEvents.
        //it's called on background from documentChanged
        //it's called on background from MyPsiTreeAnyChangeListener

        if (!isProjectValid(project) || !isValidVirtualFile(virtualFile)) {
            return
        }

        Log.log(logger::trace, "got fileChanged for {}", virtualFile)

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
            Log.warnWithException(logger, e, "Exception in fileChanged")
            ErrorReporter.getInstance().reportError(project, "${this::class.simpleName}.fileChanged", e)
        }
    }


    fun fileDeleted(virtualFile: VirtualFile?) {

        if (!isProjectValid(project)) {
            return
        }

        Backgroundable.ensurePooledThreadWithoutReadAccess {
            if (virtualFile != null) {
                buildLock.lock()
                try {
                    removeDiscoveryForFile(virtualFile)
                } finally {
                    buildLock.unlock()
                }
            }
        }
    }

    fun pathDeleted(path: String) {

        if (!isProjectValid(project)) {
            return
        }

        Backgroundable.ensurePooledThreadWithoutReadAccess {
            buildLock.lock()
            try {
                removeDiscoveryForPath(path)
            } finally {
                buildLock.unlock()
            }
        }
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


    //PsiTreeChangeEvent are fired many times for the same file, actually for every psi change in the file.
    //this listener will make sure not to call fileChanged for every event.
    //it collects the changed files and calls fileChanged every 5 seconds for the collected files.
    private inner class MyPsiTreeAnyChangeListener(val abstractNavigationDiscovery: AbstractNavigationDiscovery) : PsiTreeAnyChangeAbstractAdapter() {

        private val changeAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, abstractNavigationDiscovery)
        private val changedFiles = ConcurrentHashMap.newKeySet<VirtualFile>()

        override fun onChange(file: PsiFile?) {
            file?.let {
                val vf = it.virtualFile
                if (isValidRelevantFile(project, vf) && PsiUtils.isValidPsiFile(it) && isJvmSupportedFile(project, it)) {
                    changedFiles.add(vf)
                    changeAlarm.cancelAllRequests()
                    changeAlarm.addRequest({
                        changedFiles.forEach { file ->
                            Log.log(logger::trace, "got psi tree change for file {}", file)
                            changedFiles.remove(file)
                            abstractNavigationDiscovery.fileChanged(file)
                        }
                    }, 5000)
                }
            }
        }
    }

}