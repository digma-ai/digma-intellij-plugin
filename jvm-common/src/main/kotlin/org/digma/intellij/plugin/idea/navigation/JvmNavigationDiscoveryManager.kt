package org.digma.intellij.plugin.idea.navigation

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.IdFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import org.digma.intellij.plugin.collections.SynchronizedHashSetQueue
import org.digma.intellij.plugin.common.isValidVirtualFile
import org.digma.intellij.plugin.discovery.FileDiscoveryInfoBuilder
import org.digma.intellij.plugin.idea.index.CANDIDATE_FILES_INDEX_ID
import org.digma.intellij.plugin.idea.index.CANDIDATE_FILES_INDEX_KEY_ENDPOINT
import org.digma.intellij.plugin.idea.index.CANDIDATE_FILES_INDEX_KEY_SPAN
import org.digma.intellij.plugin.idea.index.CandidateFilesDetectionIndexListener
import org.digma.intellij.plugin.idea.index.getCandidateFilesForDiscoveryIndexInstance
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


//todo:
//on startup find candidates from the index and put in set.
//register listener on the index to listen for new and updated candidates.
//run a task that takes files from the set: builds FileInfo that contains MethodInfos with spans and endpoints.
//send the FileInfo to JvmSpanNavigationProvider and JvmEndpointNavigationProvider, each will take what they need from the FileInfo.
//dismiss the FileInfo. todo: think if i want to keep the FileInfo
//run maintenance task that checks all files in JvmSpanNavigationProvider and JvmEndpointNavigationProvider , if a file doesn't have
// index anymore assume its deleted and remove the discovery

//todo: what to do with deleted files, or files that had spans and now edited and don't have spans anymore?
// when a file is deleted the index listener will not be called, so we can't remove it from the set.'
// when a file was a candidate and edited and now is not a candidate the listener will not be called.
// possible solutions:
// 1) in the index keep an in memory list of all candidates , when a file in indexed and has no keys keys and was a candidate before fire an event of file removed.
//     this is a clean solution but keeps an in memory set of VirtualFiles
//     !! this option is not good because after the index is read and project restart there will not be indexing for files that were indexed before so can't save them in memory.
// 2) run a maintenance task every minute that check all files in the discovery, query the index if the file still has keys, if not, remove it from the discovery.
//    this needs to travers all files in the discovery, or the manager should keep a list of discovered files.


@Suppress("LightServiceMigrationCode")
class JvmNavigationDiscoveryManager(private val project: Project, private val cs: CoroutineScope) : Disposable, CandidateFilesDetectionIndexListener {

    private val logger: Logger = thisLogger()

    private var startupJob: Job? = null
    private val startupJobCompleted = AtomicBoolean(false)

    private val myLock = ReentrantLock(true)

    private val candidateFiles: Queue<VirtualFile> = SynchronizedHashSetQueue()

    private var processingJob: Job? = null

    companion object {
        @JvmStatic
        fun getInstance(project: Project): JvmNavigationDiscoveryManager {
            return project.service<JvmNavigationDiscoveryManager>()
        }
    }

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


    override fun dispose() {
        Log.trace(logger, project, "disposing")
        startupJob?.cancel(CancellationException("dispose"))
        processingJob?.cancel(CancellationException("dispose"))
        candidateFiles.clear()
    }

    fun start() {
        Log.trace(logger, project, "starting")

        getCandidateFilesForDiscoveryIndexInstance()?.addListener(this, this)

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
                            CANDIDATE_FILES_INDEX_ID,
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
                    val isInContent = readAction {
                        ProjectFileIndex.getInstance(project).isInContent(file)
                    }
                    if (!isInContent || !isValidVirtualFile(file)) {
                        candidateFiles.remove(file)
                        file = candidateFiles.peek()
                        continue
                    }

                    Log.trace(logger, project, "Processing candidate file {}", file.url)
                    val fileProcessingTime = measureTimeMillis {
                        coroutineContext.ensureActive()
                        val fileInfo = FileDiscoveryInfoBuilder.getInstance(project).buildFileInfo(file)
                        coroutineContext.ensureActive()
                        Log.trace(logger, project, "Built fileInfo for candidate file {}  [{}]", file.url, fileInfo)
                        JvmSpanNavigationProvider.getInstance(project).processCandidateFile(fileInfo)
                        JvmEndpointNavigationProvider.getInstance(project).processCandidateFile(fileInfo)
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
            JvmSpanNavigationProvider.getInstance(project).maintenance()
            JvmEndpointNavigationProvider.getInstance(project).maintenance()
        }
    }
}