package org.digma.intellij.plugin.discovery

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.ID
import com.intellij.util.indexing.IdFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import org.digma.intellij.plugin.collections.SynchronizedHashSetQueue
import org.digma.intellij.plugin.common.isValidVirtualFile
import org.digma.intellij.plugin.discovery.index.CANDIDATE_FILES_INDEX_KEY_ENDPOINT
import org.digma.intellij.plugin.discovery.index.CANDIDATE_FILES_INDEX_KEY_SPAN
import org.digma.intellij.plugin.discovery.index.CandidateFilesDetectionIndexListener
import org.digma.intellij.plugin.discovery.model.FileDiscoveryInfo
import org.digma.intellij.plugin.kotlin.ext.launchWhileActiveWithErrorReporting
import org.digma.intellij.plugin.kotlin.ext.launchWithErrorReporting
import org.digma.intellij.plugin.log.Log
import java.util.Queue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


abstract class AbstractNavigationDiscoveryManager(protected val project: Project, protected val cs: CoroutineScope) : Disposable, CandidateFilesDetectionIndexListener {

    protected val logger: Logger = thisLogger()

    private var startupJob: Job? = null
    private val startupJobCompleted = AtomicBoolean(false)

    private val myLock = ReentrantLock(true)

    private val candidateFiles: Queue<VirtualFile> = SynchronizedHashSetQueue()

    private var processingJob: Job? = null

    init {
        project.messageBus.connect(this).subscribe(DumbService.DUMB_MODE, object : DumbService.DumbModeListener {
            override fun enteredDumbMode() {
                Log.trace(logger, project, "enteredDumbMode")
                myLock.withLock {
                    if (!startupJobCompleted.get()) {
                        startupJob?.cancel(CancellationException("canceled because entered dumb mode"))
                        startupJob = null
                    }
                    processingJob?.cancel(CancellationException("canceled because entered dumb mode"))
                }
            }

            override fun exitDumbMode() {
                Log.trace(logger, project, "exitDumbMode")
                myLock.withLock {
                    if (!startupJobCompleted.get()) {
                        launchStartupJob()
                    }
                    launchCandidatesProcessingJob()
                }
            }
        })
    }


    abstract fun addIndexListener()
    abstract fun getIndexId(): ID<String, Void>
    abstract suspend fun maintenance()
    abstract suspend fun processFileInfo(fileInfo: FileDiscoveryInfo)

    override fun dispose() {
        Log.trace(logger, project, "disposing")
        startupJob?.cancel(CancellationException("dispose"))
        processingJob?.cancel(CancellationException("dispose"))
        candidateFiles.clear()
    }

    fun start() {
        Log.trace(logger, project, "starting")

        addIndexListener()

        DumbService.getInstance(project).runWhenSmart {
            //Runs on EDT!
            Log.trace(logger, project, "runWhenSmart called, launching jobs")
            launchStartupJob()
            launchCandidatesProcessingJob()
            launchMaintenanceJob()
        }
    }



    private fun launchStartupJob() {
        myLock.withLock {
            if (startupJob?.isActive == true || startupJobCompleted.get()) {
                return@withLock
            }
            Log.trace(logger, project, "launching startup job")
            startupJob?.cancel(CancellationException("new startup job started"))
            startupJob = cs.launchWithErrorReporting("${this::class.java.simpleName}.StartupJob", logger) {
                coroutineContext.ensureActive()
                Log.trace(logger, project, "Starting startup job")
                var count = 0
                val jobTime = measureTimeMillis {
                    Log.trace(logger, project, "Starting read action")
                    smartReadAction(project) {
                        Log.trace(logger, project, "Starting to process candidate files index, this may take a while..")
                        coroutineContext.ensureActive()
                        FileBasedIndex.getInstance().processFilesContainingAnyKey(
                            getIndexId(),
                            setOf(CANDIDATE_FILES_INDEX_KEY_SPAN, CANDIDATE_FILES_INDEX_KEY_ENDPOINT),
                            GlobalSearchScope.projectScope(project),
                            IdFilter.getProjectIdFilter(project, false),
                            null,
                            Processor { candidateFile ->
                                if (isValidVirtualFile(candidateFile)) {
                                    coroutineContext.ensureActive()
                                    Log.trace(logger, project, "Found candidate file {}", candidateFile.url)
                                    candidateFiles.offer(candidateFile)
                                    count++
                                }
                                true
                            })
                    }
                }

                //if we're here the job completed with no errors and no cancellation
                Log.trace(logger, project, "Finished startup job in {} ms. Found {} files", jobTime, count)
                //when all-things search finished with no cancellation or exception, dispose of the dumb mode listener.
                startupJob = null
                startupJobCompleted.set(true)

            }

            startupJob?.invokeOnCompletion { throwable ->
                if (throwable != null) {
                    Log.trace(logger, "Startup job exited with {}", throwable)
                }
            }
        }
    }



    override fun fileUpdated(file: VirtualFile, keys: List<String>) {
        Log.trace(logger, project, "fileUpdated for file {} with keys {}", file.url, keys)
        candidateFiles.offer(file)
    }


    private fun launchCandidatesProcessingJob() {
        myLock.withLock {
            if (processingJob?.isActive == true) {
                return@withLock
            }
            Log.trace(logger, project, "launching processing job")
            processingJob?.cancel(CancellationException("new job started"))
            processingJob = cs.launchWhileActiveWithErrorReporting(10.seconds, 30.seconds, "${this::class.java.simpleName}.ProcessingTask", logger) {
                coroutineContext.ensureActive()
                DumbService.getInstance(project).waitForSmartMode()
                coroutineContext.ensureActive()
                //peek the file, remove it only if processing finished successfully.
                var file = candidateFiles.peek()
                while (file != null) {
                    coroutineContext.ensureActive()
                    //skip files that are not in the project content. it's not easy to skip them in indexing
                    // because read action is not allowed in indexing.
                    val isInContent = readAction {
                        ProjectFileIndex.getInstance(project).isInContent(file)
                    }
                    if (!isInContent || !isValidVirtualFile(file)) {
                        candidateFiles.remove(file)
                        file = candidateFiles.peek()
                        continue
                    }

                    Log.trace(logger, project, "Processing candidate file {}", file.url)
                    DumbService.getInstance(project).waitForSmartMode()
                    val fileProcessingTime = measureTimeMillis {
                        coroutineContext.ensureActive()
                        val fileInfo = FileDiscoveryInfoBuilder.getInstance(project).buildFileInfo(file)
                        coroutineContext.ensureActive()
                        Log.trace(logger, project, "Built fileInfo for candidate file {}  [{}]", file.url, fileInfo)
                        processFileInfo(fileInfo)
                        candidateFiles.remove(file)
                    }
                    Log.trace(logger, project, "Finished processing candidate file {} in {} ms", file.url, fileProcessingTime)
                    coroutineContext.ensureActive()
                    file = candidateFiles.peek()
                }
            }
        }
    }




    private fun launchMaintenanceJob() {
        cs.launchWhileActiveWithErrorReporting(1.minutes, 1.minutes, "${this::class.java.simpleName}.MaintenanceTask", logger) {
            DumbService.getInstance(project).waitForSmartMode()
            maintenance()
        }
    }

}