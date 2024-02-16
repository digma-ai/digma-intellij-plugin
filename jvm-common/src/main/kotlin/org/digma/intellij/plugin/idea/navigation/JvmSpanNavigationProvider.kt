package org.digma.intellij.plugin.idea.navigation

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
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
import org.digma.intellij.plugin.idea.navigation.model.SpanLocation
import org.digma.intellij.plugin.idea.psi.isJvmSupportedFile
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.progress.RetryableTask
import org.digma.intellij.plugin.psi.PsiUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer
import java.util.function.Supplier

@Suppress("LightServiceMigrationCode") // as light service it will also register in Rider and that's not necessary
internal class JvmSpanNavigationProvider(project: Project) : AbstractNavigationDiscovery(project), Disposable {

    private val logger = Logger.getInstance(this::class.java)

    private val mySemaphore = Semaphore(1, true)

    private val spanLocations = ConcurrentHashMap(mutableMapOf<String, SpanLocation>())

    private val scheduledExecutorService = AppExecutorUtil.createBoundedScheduledExecutorService("SpanNav", 1)

    //used to restrict one update thread at a time
    private val buildSpansLock = ReentrantLock()

    private val spanNavigationDiscoveryProviders: List<SpanNavigationDiscoveryProvider> =
        listOf(OpenTelemetrySpanNavigationDiscovery(project), MicrometerSpanNavigationDiscovery(project))


    companion object {
        @JvmStatic
        fun getInstance(project: Project): JvmSpanNavigationProvider {
            return project.service<JvmSpanNavigationProvider>()
        }
    }

    override fun dispose() {
        scheduledExecutorService.shutdownNow()
    }


    fun getUrisForSpanIds(spanIds: List<String>): Map<String, Pair<String, Int>> {

        val workspaceUris = mutableMapOf<String, Pair<String, Int>>()

        spanIds.forEach(Consumer { id: String ->
            val spanLocation = spanLocations[id]
            spanLocation?.let {
                workspaceUris[id] = Pair(spanLocation.fileUri, spanLocation.offset)
            }
        })

        return workspaceUris
    }


    fun buildSpanNavigation() {
        schedule({ GlobalSearchScope.projectScope(project) }, Origin.Startup, "all")
    }


    private fun schedule(searchScopeProvider: SearchScopeProvider, origin: Origin, name: String, delayMillis: Long = 0, retry: Int = 0) {

        if (!isProjectValid(project)) {
            return
        }

        //max 20 retries and give up
        if (retry > 20) {
            return
        }

        scheduledExecutorService.schedule({
            //this semaphore is a simple way to limit the number of concurrent tasks.
            //a permit is acquired here and released by the task on its onFinish callback.
            //scheduledExecutorService is limited to one thread , buildSpanNavigationUnderProgress actually
            // starts another thread to execute the task, so this thread will wait here until the task thread is finished
            // and then will launch the next task.
            //mySemaphore.release() must be called or it will get stuck here forever.
            //this is a local handling of thread limits and not part of RetryableBackgroundTask which is agnostic
            //to the number of threads running it.
            mySemaphore.acquire()
            buildSpanNavigationUnderProgress(searchScopeProvider, origin, name, retry)
        }, delayMillis, TimeUnit.MILLISECONDS)
    }


    private fun buildSpanNavigationUnderProgress(searchScopeProvider: SearchScopeProvider, origin: Origin, name: String, retry: Int) {

        if (!isProjectValid(project)) {
            return
        }

        val stopWatch = StopWatch.createStarted()
        registerStartEvent("span-discovery-started", origin, retry)

        var context: NavigationProcessContext? = null
        val workTask = Consumer<ProgressIndicator> {
            val myContext = NavigationProcessContext(searchScopeProvider, it)
            context = myContext
            //can prepare data here if necessary
            buildSpanNavigation(myContext, origin, name, it, retry)
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
                "JvmSpanNavigationProvider.buildSpanNavigationUnderProgress.onError", it, mapOf(
                    SEVERITY_PROP_NAME to SEVERITY_MEDIUM_TRY_FIX
                )
            )
        }

        val onPCETask = Consumer<ProcessCanceledException> {
            ErrorReporter.getInstance().reportError(
                "JvmSpanNavigationProvider.buildSpanNavigationUnderProgress.onPCE", it, mapOf(
                    SEVERITY_PROP_NAME to SEVERITY_LOW_NO_FIX
                )
            )
        }

        val onFinish = Consumer<RetryableTask> { task ->

            mySemaphore.release()

            val hadProgressErrors = task.error != null
            val hadPCE = task.processCanceledException != null
            val success = task.isCompletedSuccessfully()

            if (success) {
                Log.log(logger::info, "buildSpanNavigation completed successfully")
            } else {
                if (hadProgressErrors) {
                    Log.log(logger::info, "buildSpanNavigation completed with errors")
                } else if (hadPCE && task.isExhausted()) {
                    Log.log(logger::info, "buildSpanNavigation process retry exhausted")
                } else if (hadPCE && task.isStoppedBeforeExhausted()) {
                    Log.log(logger::info, "buildSpanNavigation completed before exhausted")
                } else {
                    Log.log(logger::info, "buildSpanNavigation completed abnormally")
                }

                //if no success schedule a retry in some minutes
                schedule(searchScopeProvider, origin, name, TimeUnit.MINUTES.toMillis(2L), retry + 1)
            }

            val time = stopWatch.getTime(TimeUnit.MILLISECONDS)
            val hadErrors = context?.hasErrors() ?: false
            registerFinishedEvent("span-discovery-finished", origin, success, task.isExhausted(), retry, time, hadErrors, hadPCE)
        }


        val task = RetryableTask(
            project = project,
            title = "Digma span navigation - $origin:$name:$retry",
            workTask = workTask,
            beforeRetryTask = beforeRetryTask,
            shouldRetryTask = shouldRetryTask,
            onErrorTask = onErrorTask,
            onFinish = onFinish,
            onPCETask = onPCETask,
            maxRetries = 10,
            delayBetweenRetriesMillis = 2000L
        )

        task.runInBackground()
    }


    private fun buildSpanNavigation(context: NavigationProcessContext, origin: Origin, name: String, indicator: ProgressIndicator, retry: Int) {

        EDT.assertNonDispatchThread()
        //should not run in read action so that every section can wait for smart mode
        ReadActions.assertNotInReadAccess()

        Log.log(logger::info, "Building span navigation")

        buildSpansLock.lock()
        try {
            spanNavigationDiscoveryProviders.forEach { provider ->

                executeCatchingWithRetry(context, provider.getName(), 30000, 5) {
                    val otelSpans: Map<String, SpanLocation> = provider.discover(context)
                    spanLocations.putAll(otelSpans)
                }
                indicator.checkCanceled()
            }
        } finally {
            if (buildSpansLock.isHeldByCurrentThread) {
                buildSpansLock.unlock()
            }
        }


        //the process is about to finish. check if there were errors and schedule again from scratch.
        //these are errors that were swallowed to let the process do best effort and at least collect some spans,
        // but still probably some things didn't succeed. maybe another try will succeed. but at least we have some spans collected.
        if (context.hasErrors()) {
            context.errorsList().forEach { entry ->
                val hint = entry.key
                val errors = entry.value
                errors.forEach { err ->
                    Log.warnWithException(logger, err, "Exception in buildSpanNavigation")
                    ErrorReporter.getInstance().reportError(
                        project, "JvmSpanNavigationProvider.$hint", err, mapOf(
                            SEVERITY_PROP_NAME to SEVERITY_MEDIUM_TRY_FIX
                        )
                    )
                }
            }

            //todo: check the errors and decide if need to retry, probably useless to retry on some errors like project disposed,
            // but retry on IndexNotReadyException
            schedule(context.searchScope, origin, name, TimeUnit.MINUTES.toMillis(2L), retry + 1)
        }
    }


    fun documentChanged(document: Document) {

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
            ErrorReporter.getInstance().reportError(project, "JvmSpanNavigationProvider.documentChanged", e)
        }
    }


    /*
    This method must be called with a file that is relevant for span discovery and span navigation.
    the tests should be done before calling this method.
     */
    fun fileChanged(virtualFile: VirtualFile?) {

        if (!isProjectValid(project) || !isValidVirtualFile(virtualFile)) {
            return
        }

        Backgroundable.ensurePooledThread {
            try {
                virtualFile?.takeIf { isValidVirtualFile(virtualFile) }?.let { vf ->
                    removeDocumentSpans(vf)
                    schedule({ GlobalSearchScope.fileScope(project, virtualFile) }, Origin.FileChanged, virtualFile.name)
                }
            } catch (e: Throwable) {
                Log.warnWithException(logger, e, "Exception in fileChanged")
                ErrorReporter.getInstance().reportError(project, "JvmSpanNavigationProvider.fileChanged", e)
            }
        }
    }


    fun fileDeleted(virtualFile: VirtualFile?) {

        if (!isProjectValid(project)) {
            return
        }

        Backgroundable.ensurePooledThread {
            if (virtualFile != null) {
                buildSpansLock.lock()
                try {
                    removeDocumentSpans(virtualFile)
                } finally {
                    buildSpansLock.unlock()
                }
            }
        }
    }

    private fun removeDocumentSpans(virtualFile: VirtualFile) {
        //find all spans that are in virtualFile
        val fileSpans: Set<String> = spanLocations.entries.filter { it.value.fileUri == virtualFile.url }.map { it.key }.toSet()
        fileSpans.forEach(Consumer { key: String -> spanLocations.remove(key) })
    }


}
