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
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.EndpointDiscovery
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.EndpointDiscoveryService
import org.digma.intellij.plugin.idea.psi.isJvmSupportedFile
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.EndpointInfo
import org.digma.intellij.plugin.progress.RetryableTask
import org.digma.intellij.plugin.psi.PsiUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.function.Supplier

@Suppress("LightServiceMigrationCode") // as light service it will also register in Rider and that's not necessary
internal class JvmEndpointNavigationProvider(project: Project) : AbstractNavigationDiscovery(project), Disposable {

    private val logger = Logger.getInstance(this::class.java)

    private val endpointsMap = ConcurrentHashMap(mutableMapOf<String, MutableSet<EndpointInfo>>())

    private val scheduledExecutorService = AppExecutorUtil.createBoundedScheduledExecutorService("EndpointNav", 1)

    //used to restrict one update thread at a time
    private val buildEndpointLock = ReentrantLock()


    companion object {
        @JvmStatic
        fun getInstance(project: Project): JvmEndpointNavigationProvider {
            return project.service<JvmEndpointNavigationProvider>()
        }
    }

    override fun dispose() {
        scheduledExecutorService.shutdownNow()
    }


    fun getEndpointInfos(endpointId: String?): Set<EndpointInfo> {
        val endpointInfos = endpointsMap[endpointId] ?: return setOf()
        // cloning the result, to keep consistency
        return endpointInfos.toSet()
    }


    fun buildEndpointNavigation() {
        schedule({ GlobalSearchScope.projectScope(project) }, Origin.Startup)
    }

    private fun schedule(searchScopeProvider: SearchScopeProvider, origin: Origin, delayMillis: Long = 0, retry: Int = 0) {

        if (!isProjectValid(project)) {
            return
        }

        //max 20 retries and give up
        if (retry > 20) {
            return
        }

        scheduledExecutorService.schedule({
            buildEndpointNavigationUnderProgress(searchScopeProvider, origin, retry)
        }, delayMillis, TimeUnit.MILLISECONDS)
    }


    private fun buildEndpointNavigationUnderProgress(searchScopeProvider: SearchScopeProvider, origin: Origin, retry: Int) {

        if (!isProjectValid(project)) {
            return
        }

        val stopWatch = StopWatch.createStarted()
        registerStartEvent("endpoint-discovery-started", origin, retry)

        var context: NavigationProcessContext? = null
        val workTask = Consumer<ProgressIndicator> {
            context = NavigationProcessContext(searchScopeProvider, it)
            //can prepare data here if necessary
            buildEndpointNavigation(context!!, origin, it, retry)
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
                "JvmEndpointNavigationProvider.buildEndpointNavigationUnderProgress.onError", it, mapOf(
                    SEVERITY_PROP_NAME to SEVERITY_MEDIUM_TRY_FIX
                )
            )
        }

        val onPCETask = Consumer<ProcessCanceledException> {
            ErrorReporter.getInstance().reportError(
                "JvmEndpointNavigationProvider.buildEndpointNavigationUnderProgress.onPCE", it, mapOf(
                    SEVERITY_PROP_NAME to SEVERITY_LOW_NO_FIX
                )
            )
        }

        //onFinish will be called for each retry , can check properties on the task to decide
        // if all retries are finished, actually task.exhausted
        val onFinish = Consumer<RetryableTask> { task ->

            val hadProgressErrors = task.error != null
            val hadPCE = task.processCanceledException != null
            val success = task.isCompletedSuccessfully()

            if (success) {
                Log.log(logger::info, "buildEndpointNavigation completed successfully")
            } else {
                if (hadProgressErrors) {
                    Log.log(logger::info, "buildEndpointNavigation completed with errors")
                } else if (hadPCE && task.isExhausted()) {
                    Log.log(logger::info, "buildEndpointNavigation process retry exhausted")
                } else if (hadPCE && task.isStoppedBeforeExhausted()) {
                    Log.log(logger::info, "buildEndpointNavigation completed before exhausted")
                } else {
                    Log.log(logger::info, "buildEndpointNavigation completed abnormally")
                }

                //if no success schedule a retry in some minutes
                schedule(searchScopeProvider, origin, TimeUnit.MINUTES.toMillis(2L), retry + 1)
            }

            val time = stopWatch.getTime(TimeUnit.MILLISECONDS)
            val hadErrors = context?.hasErrors() ?: false
            registerFinishedEvent("endpoint-discovery-finished", origin, success, task.isExhausted(), retry, time, hadErrors, hadPCE)
        }


        val task = RetryableTask(
            project = project,
            title = "Digma endpoint navigation - $origin:$retry",
            workTask = workTask,
            beforeRetryTask = beforeRetryTask,
            shouldRetryTask = shouldRetryTask,
            onErrorTask = onErrorTask,
            onFinish = onFinish,
            onPCETask = onPCETask,
            maxRetries = 3, //todo: change to 10 or more
            delayBetweenRetriesMillis = 2000L
        )

        task.runInBackground()

    }


    private fun buildEndpointNavigation(context: NavigationProcessContext, origin: Origin, indicator: ProgressIndicator, retry: Int) {

        EDT.assertNonDispatchThread()
        //should not run in read action so that every section can wait for smart mode
        ReadActions.assertNotInReadAccess()

        Log.log(logger::info, "Building endpoint navigation")

        buildEndpointLock.lock()
        try {

            //some frameworks may fail. for example ktor will fail if kotlin plugin is disabled
            val endpointDiscoveries = EndpointDiscoveryService.getInstance(project).getAllEndpointDiscovery()

            endpointDiscoveries.forEach { endpointDiscovery: EndpointDiscovery ->

                executeCatchingWithRetry(context, endpointDiscovery.getName(), 30000, 5) {
                    val endpointInfos = endpointDiscovery.lookForEndpoints(context.searchScope, context)
                    endpointInfos?.forEach {
                        addToMethodsMap(it)
                    }
                }

                indicator.checkCanceled()
            }
        } finally {
            if (buildEndpointLock.isHeldByCurrentThread) {
                buildEndpointLock.unlock()
            }
        }


        //the process is about to finish. check if there were errors and schedule again from scratch.
        //these are errors that were swallowed to let the process do best effort and at least collect some endpoints,
        // but still probably some things didn't succeed. maybe another try will succeed. but at least we have some endpoints collected.
        if (context.hasErrors()) {
            context.errorsList().forEach { entry ->
                val hint = entry.key
                val errors = entry.value
                errors.forEach { err ->
                    Log.warnWithException(logger, err, "Exception in buildEndpointNavigation")
                    ErrorReporter.getInstance().reportError(
                        project, "JvmEndpointNavigationProvider.$hint", err, mapOf(
                            SEVERITY_PROP_NAME to SEVERITY_MEDIUM_TRY_FIX
                        )
                    )
                }
            }

            //todo: check the errors and decide if need to retry, probably useless to retry on some errors like project disposed,
            // but retry on IndexNotReadyException
            schedule(context.searchScope, origin, TimeUnit.MINUTES.toMillis(2L), retry + 1)
        }
    }


    private fun addToMethodsMap(endpointInfo: EndpointInfo) {
        val methods = endpointsMap.computeIfAbsent(endpointInfo.id) { mutableSetOf() }
        methods.add(endpointInfo)
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
            ErrorReporter.getInstance().reportError(project, "JvmEndpointNavigationProvider.documentChanged", e)
        }
    }


    /*
   This method must be called with a file that is relevant for endpoints discovery and endpoints navigation.
   the tests should be done before calling this method.
    */
    fun fileChanged(virtualFile: VirtualFile?) {

        if (!isProjectValid(project) || !isValidVirtualFile(virtualFile)) {
            return
        }

        Backgroundable.ensurePooledThread {
            try {
                virtualFile?.takeIf { isValidVirtualFile(virtualFile) }?.let { vf ->
                    removeDocumentEndpoint(vf)
                    schedule({ GlobalSearchScope.fileScope(project, virtualFile) }, Origin.FileChanged)
                }
            } catch (e: Throwable) {
                Log.warnWithException(logger, e, "Exception in fileChanged")
                ErrorReporter.getInstance().reportError(project, "JvmEndpointNavigationProvider.fileChanged", e)
            }
        }
    }


    fun fileDeleted(virtualFile: VirtualFile?) {

        if (!isProjectValid(project)) {
            return
        }

        Backgroundable.ensurePooledThread {
            if (virtualFile != null) {
                buildEndpointLock.lock()
                try {
                    removeDocumentEndpoint(virtualFile)
                } finally {
                    buildEndpointLock.unlock()
                }
            }
        }
    }

    private fun removeDocumentEndpoint(virtualFile: VirtualFile) {
        val filePredicate = FilePredicate(virtualFile.url)
        for (methods in endpointsMap.values) {
            methods.removeIf(filePredicate)
        }
    }


    private class FilePredicate(private val theFileUri: String) : Predicate<EndpointInfo> {
        override fun test(endpointInfo: EndpointInfo): Boolean {
            return theFileUri == endpointInfo.containingFileUri
        }
    }


}