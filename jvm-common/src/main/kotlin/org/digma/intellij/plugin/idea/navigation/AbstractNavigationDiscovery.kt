package org.digma.intellij.plugin.idea.navigation

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.concurrency.AppExecutorUtil
import kotlinx.datetime.Clock
import org.apache.commons.lang3.time.StopWatch
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.ReadActions
import org.digma.intellij.plugin.common.SearchScopeProvider
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.common.isValidVirtualFile
import org.digma.intellij.plugin.common.runInReadAccessWithResult
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.errorreporting.SEVERITY_LOW_NO_FIX
import org.digma.intellij.plugin.errorreporting.SEVERITY_MEDIUM_TRY_FIX
import org.digma.intellij.plugin.errorreporting.SEVERITY_PROP_NAME
import org.digma.intellij.plugin.idea.navigation.model.NavigationProcessContext
import org.digma.intellij.plugin.idea.navigation.model.Origin
import org.digma.intellij.plugin.idea.psi.isJvmSupportedFile
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.posthog.SessionMetadata
import org.digma.intellij.plugin.posthog.getPluginLoadedKey
import org.digma.intellij.plugin.progress.RetryableTask
import org.digma.intellij.plugin.psi.PsiUtils
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer
import java.util.function.Supplier
import kotlin.time.Duration

abstract class AbstractNavigationDiscovery(protected val project: Project) : Disposable {

    protected val logger = Logger.getInstance(this::class.java)

    //used to restrict one update thread at a time
    protected val buildLock = ReentrantLock()

    private val scheduledExecutorService = AppExecutorUtil.createBoundedScheduledExecutorService("SpanNav", 1)


    override fun dispose() {
        scheduledExecutorService.shutdownNow()
    }

    abstract val type: String

    abstract fun removeDiscoveryForFile(file: VirtualFile)

    abstract fun getTask(myContext: NavigationProcessContext, origin: Origin, name: String, indicator: ProgressIndicator, retry: Int): Runnable


    //navigation discovery will run on startup using a single thread scheduler and invisible retryable task.
    // the task will retry a few times in case of errors but in the meantime will collect discovery objects and store them.
    //there are two levels of retry, the task itself is retryable and will retry every few seconds if the process failed.
    //if failed after all attempts a new task will be scheduled to run after 2 minutes in hope it will complete with no errors.
    //max 20 attempts to complete with no errors.
    //on file changed the same thing will happen for one file.
    fun buildNavigationDiscovery() {
        schedule({ GlobalSearchScope.projectScope(project) }, null, Origin.Startup, "all")
    }


    protected fun schedule(
        searchScopeProvider: SearchScopeProvider,
        preTask: Runnable?, origin: Origin, name: String, delayMillis: Long = 0, retry: Int = 0,
    ) {

        //this method should be very fast, only schedule a task. it may be called on EDT.

        if (!isProjectValid(project)) {
            return
        }

        //max 20 retries and give up
        if (retry > 20) {
            return
        }

        //scheduledExecutorService is single thread and thus only one thread at a time will process scheduled tasks.
        // which means that scheduling is not accurate and does not need to be in this case.
        scheduledExecutorService.schedule({
            //run the preTask on same thread before the main task
            preTask?.run()
            buildNavigationUnderProgress(searchScopeProvider, origin, name, retry)
        }, delayMillis, TimeUnit.MILLISECONDS)
    }


    private fun buildNavigationUnderProgress(searchScopeProvider: SearchScopeProvider, origin: Origin, name: String, retry: Int) {

        if (!isProjectValid(project)) {
            return
        }

        val stopWatch = StopWatch.createStarted()
        registerStartEvent("$type-discovery-started", origin, retry)

        var context: NavigationProcessContext? = null
        val workTask = Consumer<ProgressIndicator> {
            val myContext = NavigationProcessContext(searchScopeProvider, it)
            context = myContext
            getTask(myContext, origin, name, it, retry).run()
        }

        val beforeRetryTask = Consumer<RetryableTask> { task ->
            //this is a hook before every retry to change anything on the task,
            // may even change the work task or anything else.

            //increase the delay on every retry
            task.delayBetweenRetriesMillis += 5000L
        }

        val shouldRetryTask = Supplier<Boolean> {
            //a hook to stop retrying
            isProjectValid(project)
        }

        val onErrorTask = Consumer<Throwable> {
            ErrorReporter.getInstance().reportError(
                "${this::class.simpleName}.buildNavigationUnderProgress.onError", it, mapOf(
                    SEVERITY_PROP_NAME to SEVERITY_MEDIUM_TRY_FIX
                )
            )
        }

        val onPCETask = Consumer<ProcessCanceledException> {
            ErrorReporter.getInstance().reportError(
                "${this::class.simpleName}.buildNavigationUnderProgress.onPCE", it, mapOf(
                    SEVERITY_PROP_NAME to SEVERITY_LOW_NO_FIX
                )
            )
        }

        val onFinish = Consumer<RetryableTask> { task ->

            val hadProgressErrors = task.error != null
            val hadPCE = task.processCanceledException != null
            val success = task.isCompletedSuccessfully()

            if (success) {
                Log.log(logger::info, "${this::class.simpleName} completed successfully")
            } else {
                if (hadProgressErrors) {
                    Log.log(logger::info, "${this::class.simpleName} completed with errors")
                } else if (hadPCE && task.isExhausted()) {
                    Log.log(logger::info, "${this::class.simpleName} process retry exhausted")
                } else if (hadPCE && task.isStoppedBeforeExhausted()) {
                    Log.log(logger::info, "${this::class.simpleName} completed before exhausted")
                } else {
                    Log.log(logger::info, "${this::class.simpleName} completed abnormally")
                }

                //if no success schedule a retry in some minutes.
                // no success means an error in the process, process cancellation or some other error.
                // try again, and maybe it will have more success. the schedule method limits maximum 20 retries.
                schedule(searchScopeProvider, null, origin, name, TimeUnit.MINUTES.toMillis(2L), retry + 1)
            }

            val time = stopWatch.getTime(TimeUnit.MILLISECONDS)
            val hadErrors = context?.hasErrors() ?: false
            registerFinishedEvent("$type-discovery-finished", origin, success, task.isExhausted(), retry, time, hadErrors, hadPCE)
        }


        val task = RetryableTask.Invisible(
            project = project,
            title = "$type discovery $name", //title is ignored in RetryableTask.Invisible but may be used for logging
            workTask = workTask,
            beforeRetryTask = beforeRetryTask,
            shouldRetryTask = shouldRetryTask,
            onErrorTask = onErrorTask,
            onFinish = onFinish,
            onPCETask = onPCETask,
            maxRetries = 10,
            delayBetweenRetriesMillis = 2000L
        )

        //very important, it will run on the scheduler thread which is limited to one thread at a time
        task.reuseCurrentThread = true
        task.runInBackground()
    }


    protected fun handleErrorsInProcess(context: NavigationProcessContext, origin: Origin, name: String, retry: Int) {
        //the process is about to finish. check if there were errors and schedule again from scratch.
        //these are errors that were swallowed to let the process do best effort and at least collect some spans,
        // but still probably some things didn't succeed. maybe another try will succeed. but at least we have some spans collected.
        if (context.hasErrors()) {
            context.errorsList().forEach { entry ->
                val hint = entry.key
                val errors = entry.value
                errors.forEach { err ->
                    Log.warnWithException(logger, err, "Exception in build $type navigation")
                    ErrorReporter.getInstance().reportError(
                        project, "${this::class.simpleName}.$hint", err, mapOf(
                            SEVERITY_PROP_NAME to SEVERITY_MEDIUM_TRY_FIX
                        )
                    )
                }
            }

            //if there were errors in the process schedule another try. the first run already inserted discovery
            // to spanLocations , but maybe there were errors for some files or index not ready somewhere,
            // try again, and maybe it will have more success. the schedule method limits maximum 20 retries.
            schedule(context.searchScope, null, origin, name, TimeUnit.MINUTES.toMillis(2L), retry + 1)
        }
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
                if (PsiUtils.isValidPsiFile(psiFile) && isJvmSupportedFile(project, it)) {
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

        //this method may be called on EDT,it should be very fast and only schedule a task for the changed file.
        //it's called on EDT from BulkFileChangeListenerForJvmSpanNavigation.processEvents because we don't want to
        // start a new thread for every file.
        //it's called on background when called from documentChanged

        if (!isProjectValid(project) || !isValidVirtualFile(virtualFile)) {
            return
        }

        try {
            virtualFile?.takeIf { isValidVirtualFile(virtualFile) }?.let { vf ->
                //must remove the document spans before building discovery for that file again,
                // the preTask sent to schedule will do it on the same thread where the task will run
                schedule({ GlobalSearchScope.fileScope(project, virtualFile) }, { removeDiscoveryForFile(vf) }, Origin.FileChanged, vf.name)
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

        Backgroundable.ensurePooledThread {
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


    private fun registerStartEvent(eventName: String, origin: Origin, retry: Int) {

        //no need to send the event on file changed because it is too many
        if (origin == Origin.FileChanged) {
            return
        }


        val pluginLoadedTime = SessionMetadata.getInstance().getCreated(getPluginLoadedKey(project))
        val duration = pluginLoadedTime?.let {
            Clock.System.now().minus(it)
        } ?: Duration.ZERO

        val startupLagMillis = duration.inWholeMilliseconds

        val startupLagToShow =
            if (startupLagMillis > 2000) "${TimeUnit.MILLISECONDS.toSeconds(startupLagMillis)} seconds" else "$startupLagMillis millis"

        val details = if (origin == Origin.Startup) {
            mapOf(
                "startupLag" to startupLagToShow,
                "origin" to origin,
                "retry" to retry
            )
        } else {
            mapOf(
                "origin" to origin,
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
        origin: Origin,
        success: Boolean,
        exhaustedRetries: Boolean,
        retry: Int,
        timeTookMillis: Long,
        hadErrors: Boolean,
        hadPCE: Boolean,
    ) {

        //no need to send the event on file changed because it is too many
        if (origin == Origin.FileChanged) {
            return
        }

        val timeToShow = if (timeTookMillis > 2000) "${TimeUnit.MILLISECONDS.toSeconds(timeTookMillis)} seconds" else "$timeTookMillis millis"

        ActivityMonitor.getInstance(project).registerJvmNavigationDiscoveryEvent(
            eventName,
            mapOf(
                "timeTook" to timeToShow,
                "origin" to origin,
                "success" to success,
                "exhaustedRetries" to exhaustedRetries,
                "retry" to retry,
                "hadErrors" to hadErrors,
                "hadPCE" to hadPCE
            )
        )
    }


}