package org.digma.intellij.plugin.codelens.provider

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import io.ktor.util.collections.ConcurrentMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.common.isValidVirtualFile
import org.digma.intellij.plugin.document.DocumentInfoStorage
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.lens.CodeLens
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.minutes

@Service(Service.Level.PROJECT)
class CodeLensProvider(private val project: Project, private val cs: CoroutineScope) : Disposable {

    private val logger = Logger.getInstance(this::class.java)

    private val codeLensBuilder = CodeLensBuilder(project)
    private val codeLensCache: ConcurrentMap<VirtualFile, Set<CodeLens>> = ConcurrentMap()
    private val runningLoadJobs: ConcurrentMap<VirtualFile, Job> = ConcurrentMap()
    private val runningRefreshJobs: MutableSet<Job> = ConcurrentHashMap.newKeySet()

    companion object {
        @JvmStatic
        fun getInstance(project: Project): CodeLensProvider {
            return project.service<CodeLensProvider>()
        }
    }

    init {
        cs.launch {
            delay(1.minutes)
            refresh()
        }
    }

    override fun dispose() {
        runningLoadJobs.values.forEach { it.cancel(CancellationException("CodeLensProvider is disposing")) }
        runningRefreshJobs.forEach { it.cancel(CancellationException("CodeLensProvider is disposing")) }
        codeLensCache.clear()
    }

    internal fun loadCodeLens(file: VirtualFile, documentInfo: DocumentInfo) {
        //don't launch if there is no connection to backend
        if (BackendConnectionMonitor.getInstance(project).isConnectionError()) return

        runningLoadJobs[file]?.cancel(CancellationException("CodeLensProvider.loadCodeLens called again before previous job finished"))
        val job = cs.launch {
            if (!isValidVirtualFile(file)) return@launch
            Log.log(logger::trace, "loading code lens for file {}", file)
            loadCodeLensInternal(file, documentInfo)
        }
        runningLoadJobs[file] = job
        job.invokeOnCompletion { cause ->
            runningLoadJobs.remove(file)
            //if the cause is not null and not CancellationException, then it means the job failed,
            if (cause != null && cause !is CancellationException) {
                Log.warnWithException(logger, project, cause, "loadCodeLens.launch: job failed {}", cause)
                ErrorReporter.getInstance().reportError(project, "CodeLensProvider.loadCodeLens", cause)
            }
        }
    }

    private suspend fun loadCodeLensInternal(file: VirtualFile, documentInfo: DocumentInfo) {
        coroutineContext.ensureActive()
        if (logger.isTraceEnabled) {
            Log.log(logger::trace, "loading code lens for file {}", file)
        }
        val codeLens = codeLensBuilder.buildCodeLens(documentInfo)
        if (logger.isTraceEnabled) {
            Log.log(logger::trace, "code lens for file {} is {}", file, codeLens)
        }
        coroutineContext.ensureActive()
        val previousCodeLens = codeLensCache[file]
        codeLensCache[file] = codeLens
        if (previousCodeLens != codeLens) {
            coroutineContext.ensureActive()
            if (logger.isTraceEnabled) {
                Log.log(logger::trace, "code lens changed for file {}", file)
            }
            project.messageBus.syncPublisher(CodeLensChanged.CODELENS_CHANGED_TOPIC).codelensChanged(file)
        }
    }


    internal fun removeCodeLens(file: VirtualFile) {
        runningLoadJobs[file]?.cancel()
        cs.launch {
            Log.log(logger::trace, "removing code lens for file {}", file)
            codeLensCache.remove(file)
            project.messageBus.syncPublisher(CodeLensChanged.CODELENS_CHANGED_TOPIC).codelensRemoved(file)
        }
    }

    internal fun clearCodeLens() {
        cs.launch {
            Log.log(logger::trace, "clearing code lens")
            runningLoadJobs.values.forEach { it.cancel(CancellationException("CodeLensProvider.clearCodeLens called before previous job finished")) }
            runningRefreshJobs.forEach { it.cancel(CancellationException("CodeLensProvider.clearCodeLens called before previous job finished")) }
            codeLensCache.clear()
            project.messageBus.syncPublisher(CodeLensChanged.CODELENS_CHANGED_TOPIC).codelensCleared()
        }
    }

    internal fun refresh() {
        //don't launch if there is no connection to backend
        if (BackendConnectionMonitor.getInstance(project).isConnectionError()) return

        runningRefreshJobs.forEach { it.cancel(CancellationException("CodeLensProvider.refresh called again before previous job finished")) }
        val job = cs.launch {
            Log.log(logger::trace, "refreshing code lens")
            val files = codeLensCache.keys.toList()
            files.forEach { file ->
                val documentInfo = DocumentInfoStorage.getInstance(project).getDocumentInfo(file)
                if (documentInfo != null) {
                    Log.log(logger::trace, "refreshing code lens for file {}", file)
                    loadCodeLens(file, documentInfo)
                } else {
                    Log.log(logger::trace, "no document info to refresh for file {}", file)
                }
            }
        }
        runningRefreshJobs.add(job)
        job.invokeOnCompletion { cause ->
            runningRefreshJobs.remove(job)
            //if the cause is not null and not CancellationException, then it means the job failed,
            if (cause != null && cause !is CancellationException) {
                Log.warnWithException(logger, project, cause, "CodeLensProvider.refresh: job failed {}", cause)
                ErrorReporter.getInstance().reportError(project, "CodeLensProvider.refresh", cause)
            }
        }
    }

    fun getCodeLens(file: VirtualFile): Set<CodeLens> {
        return codeLensCache.getOrPut(file) { emptySet() }
    }


}