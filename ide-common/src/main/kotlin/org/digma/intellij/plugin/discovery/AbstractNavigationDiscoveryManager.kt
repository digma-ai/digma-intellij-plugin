package org.digma.intellij.plugin.discovery

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.waitForSmartMode
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileManagerListener
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.ID
import com.intellij.util.indexing.IdFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.digma.intellij.plugin.collections.SynchronizedHashSetQueue
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.common.isValidVirtualFile
import org.digma.intellij.plugin.common.measureTimeMillisWithResult
import org.digma.intellij.plugin.discovery.index.CANDIDATE_FILES_INDEX_KEY_ENDPOINT
import org.digma.intellij.plugin.discovery.index.CANDIDATE_FILES_INDEX_KEY_SPAN
import org.digma.intellij.plugin.discovery.index.CandidateFilesDetectionIndexListener
import org.digma.intellij.plugin.discovery.model.FileDiscoveryInfo
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.kotlin.ext.launchWhileActiveWithErrorReporting
import org.digma.intellij.plugin.kotlin.ext.launchWithErrorReporting
import org.digma.intellij.plugin.log.Log
import java.util.Queue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


abstract class AbstractNavigationDiscoveryManager(protected val project: Project, protected val cs: CoroutineScope) : Disposable,
    CandidateFilesDetectionIndexListener {

    protected val logger: Logger = Logger.getInstance(this::class.java)

    private var startupJob: Job? = null
    private val startupJobCompleted = AtomicBoolean(false)

    private val myStopStartMutex = Mutex()

    private val candidateFiles: Queue<VirtualFile> = SynchronizedHashSetQueue()
    private val discoveryErrorFiles: MutableMap<String, Int> = object : LinkedHashMap<String, Int>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Int>): Boolean {
            return size > 1000
        }
    }

    private var processingJob: Job? = null
    private var maintenanceJob: Job? = null
    private var restartJob: Job? = null
    private var statusJob: Job? = null

    private val jonManager = JobManager()


    companion object {
        fun isDiscoveryEnabled(): Boolean {
            return java.lang.Boolean.parseBoolean(System.getProperty("org.digma.discovery.enabled", "true"))
        }
    }

    init {

        if (logger.isTraceEnabled) {
            launchStatusJob()
        }

        project.messageBus.connect(this).subscribe(DumbService.DUMB_MODE, object : DumbService.DumbModeListener {
            //isDumbMode is meant to protect against asymmetric bugs in the event. for example, if there are multiple
            // enteredDumbMode but not exitDumbMode in between. there is no need to call jonManager.stop if it's already called,
            // but jonManager.startWithDelay was not yet called from this listener.
            private val isDumbMode = AtomicBoolean(false)
            override fun enteredDumbMode() {
                if (isDumbMode.compareAndSet(false, true)) {
                    Log.trace(logger, project, "Event: enteredDumbMode")
                    jonManager.stop("stop because entered dumb mode")
                }
            }

            override fun exitDumbMode() {
                if (isDumbMode.compareAndSet(true, false)) {
                    Log.trace(logger, project, "Event: exitDumbMode")
                    jonManager.startWithDelay()
                }
            }
        })

//this listener send too many events
//        VirtualFileManager.getInstance().addAsyncFileListener({ it ->
//            val count = it.size
//            object : AsyncFileListener.ChangeApplier {
//                override fun beforeVfsChange() {
////                    if(count > 10) {
//                    Log.trace(logger, project, "Event: beforeVfsChange, count=$count")
//                    jonManager.stop("stop because beforeVfsChange")
////                    }
//                }
//
//                override fun afterVfsChange() {
////                    if (count > 10) {
//                    Log.trace(logger, project, "Event: afterVfsChange, count=$count")
//                    jonManager.startWithDelay()
////                    }
//                }
//            }
//        }, this)


        VirtualFileManager.getInstance().addVirtualFileManagerListener(object : VirtualFileManagerListener {
            //isInRefresh is meant to protect against asymmetric bugs in the event. for example, if there are multiple
            // beforeRefreshStart but not afterRefreshFinish in between. there is no need to call jonManager.stop if it's already called,
            // and jonManager.startWithDelay was not yet called from this listener.
            private val isInRefresh = AtomicBoolean(false)
            override fun beforeRefreshStart(asynchronous: Boolean) {
                if (isInRefresh.compareAndSet(false, true)) {
                    Log.trace(logger, project, "Event: beforeRefreshStart")
                    jonManager.stop("stop because beforeRefreshStart")
                }
            }

            override fun afterRefreshFinish(asynchronous: Boolean) {
                if (isInRefresh.compareAndSet(true, false)) {
                    Log.trace(logger, project, "Event: afterRefreshFinish")
                    jonManager.startWithDelay()
                }
            }
        }, this)
    }


    abstract fun addIndexListener()
    abstract fun getIndexId(): ID<String, Void>
    abstract suspend fun maintenance()
    abstract suspend fun processFileInfo(fileInfo: FileDiscoveryInfo)
    abstract suspend fun getDiscoveryStatus(): String


    private sealed class JobCommand {
        data class Stop(val reason: String) : JobCommand()
        object RestartWithDelay : JobCommand()
        object Dispose : JobCommand()
    }

    private inner class JobManager {

        private val stopStartCounter = AtomicInteger(0)

        private val jobManagementChannel = Channel<JobCommand>(Channel.UNLIMITED)
        private val managementJob: Job
        private var stuckTimerJob: Job? = null

        init {
            managementJob = cs.launchWithErrorReporting("${this::class.java.simpleName}.JobManager", logger){
                Log.trace(logger, project, "JobManager: managementJob started")
                jobManagementChannel.consumeEach { command: JobCommand ->
                    Log.trace(logger, project, "JobManager: processing command: {}", command::class.java.simpleName)
                    when (command) {
                        is JobCommand.Stop -> {
                            Log.trace(logger, project, "JobManager: processing Stop command: {}", command.reason)
                            stopJobs(command.reason)
                            startStuckTimer()
                        }

                        is JobCommand.RestartWithDelay -> {
                            Log.trace(logger, project, "JobManager: processing RestartWithDelay command")
                            //Allow restart only when stopStartCounter is 0. that means each listener that called stop also called startWithDelay.
                            //If stopStartCounter is higher than 0, it means a listener called stop but didn't call startWithDelay yet.
                            if (stopStartCounter.get() == 0) {
                                stuckTimerJob?.cancel()
                                restartJobsWithDelay()
                            }
                        }

                        is JobCommand.Dispose -> {
                            Log.trace(logger, project, "JobManager: processing Dispose command")
                            stopJobs("disposed")
                        }
                    }
                }
            }
        }

        //JobManager relies on the event listeners to be consistent in stop/startWithDelay. Meaning every
        // stop must be eventually followed by startWithDelay. enteredDumbMode must be followed by exitDumbMode,
        // beforeRefreshStart must be followed by afterRefreshFinish etc. if that doesn't happen, for example, if
        // beforeRefreshStart is called but never followed by afterRefreshFinish the jobs will never restart. It should not happen
        // unless there is a bug or inconsistencies in intellij events.
        //The solution is to start a timer every time a stop is called and count 5 minutes, if after 5 minutes the stopStartCounter
        // is not 0, then restart the jobs and hope for the best.
        private fun startStuckTimer() {
            stuckTimerJob?.cancel()
            stuckTimerJob = cs.launchWithErrorReporting("$ {this::class.java.simpleName}.JobManager.startStuckTimer", logger){
                delay(5.minutes)
                @Suppress("UnstableApiUsage")
                project.waitForSmartMode()
                coroutineContext.ensureActive()
                if (stopStartCounter.get() > 0) {
                    stopStartCounter.set(0)
                    Log.warn(logger, project, "JobManager: jobs were stopped for over 5 minutes — auto-restarting. Possible missed event?")
                    restartJobsWithDelay()
                }
            }
        }

        @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
        fun status(): String {
            return "stopStartCounter: ${stopStartCounter.get()}, managementJob: ${managementJob.isActive}, stuckTimerJob: ${stuckTimerJob?.isActive}, jobManagementChannel: [Empty:${jobManagementChannel.isEmpty},ClosedForSend:${jobManagementChannel.isClosedForSend},ClosedForReceive:${jobManagementChannel.isClosedForReceive}]"
        }

        fun startup() {
            cs.launchWithErrorReporting("${this::class.java.simpleName}.JobManager.startup", logger){
                //on startup currentState is not relevant, if its 0 the jobs will start, if it was changed by any listener,
                // the jobs will not start until the currentState is 0 again when a listener calls startWithDelay
                Log.trace(logger, project, "JobManager: startup called, sending RestartWithDelay command")
                val result = jobManagementChannel.trySend(JobCommand.RestartWithDelay)
                if (result.isSuccess) {
                    Log.trace(logger, project, "JobManager: RestartWithDelay command result is success")
                } else {
                    Log.warn(logger, project, "JobManager: RestartWithDelay command failed with ${result.exceptionOrNull()}")
                    ErrorReporter.getInstance()
                        .reportError("AbstractNavigationDiscoveryManager.JobManager.startup", result.exceptionOrNull() ?: Exception("Unknown error"))
                }
            }
        }

        fun startWithDelay() {
            cs.launchWithErrorReporting("${this::class.java.simpleName}.JobManager.startWithDelay", logger) {
                val state = if (stopStartCounter.get() > 0) {
                    stopStartCounter.decrementAndGet()
                } else {
                    stopStartCounter.get()
                }
                Log.trace(logger, project, "JobManager: startWithDelay called and state is {}, sending RestartWithDelay command", state)
                val result = jobManagementChannel.trySend(JobCommand.RestartWithDelay)
                if (result.isSuccess) {
                    Log.trace(logger, project, "JobManager: RestartWithDelay command result is success")
                } else {
                    Log.warn(logger, project, "JobManager: RestartWithDelay command failed with ${result.exceptionOrNull()}")
                    ErrorReporter.getInstance()
                        .reportError("AbstractNavigationDiscoveryManager.JobManager.startWithDelay", result.exceptionOrNull() ?: Exception("Unknown error"))
                }
            }
        }

        fun stop(reason: String) {
            cs.launchWithErrorReporting("${this::class.java.simpleName}.JobManager.stop", logger) {
                restartJob?.cancel(CancellationException("stop called with reason $reason"))
                val state = stopStartCounter.incrementAndGet()
                Log.trace(logger, project, "JobManager: stop called with reason: {},  and state {}, sending Stop command", reason, state)
                val result = jobManagementChannel.trySend(JobCommand.Stop(reason))
                if (result.isSuccess) {
                    Log.trace(logger, project, "JobManager: Stop command result is success")
                } else {
                    Log.warn(logger, project, "JobManager: Stop command failed with ${result.exceptionOrNull()}")
                    ErrorReporter.getInstance()
                        .reportError("AbstractNavigationDiscoveryManager.JobManager.stop", result.exceptionOrNull() ?: Exception("Unknown error"))
                }
            }
        }

        fun dispose() {
            //on dispose cs may already be inactive so just send on current thread
            Log.trace(logger, project, "JobManager: dispose called,sending Dispose command")
            stuckTimerJob?.cancel()
            val result = jobManagementChannel.trySend(JobCommand.Dispose)
            Log.trace(logger, project, "JobManager: Dispose command result is success:{}: {}", result.isSuccess, result)
            jobManagementChannel.close()
            managementJob.cancel()
        }
    }


    fun startup() {
        Log.trace(logger, project, "starting up")
        addIndexListener()
        DumbService.getInstance(project).runWhenSmart {
            //Runs on EDT!
            Log.trace(logger, project, "runWhenSmart called on startup, launching jobs")
            jonManager.startup()
        }
    }


    private fun stopJobs(reason: String) {
        Log.trace(logger, project, "stopJobs called with reason {}", reason)
        restartJob?.cancel()
        cs.launchWithErrorReporting("${this::class.java.simpleName}.stopJobs", logger) {
            myStopStartMutex.withLock {
                if (!startupJobCompleted.get()) {
                    startupJob?.cancel(CancellationException(reason))
                    startupJob = null
                }
                restartJob?.cancel(CancellationException(reason))
                processingJob?.cancel(CancellationException(reason))
                maintenanceJob?.cancel(CancellationException(reason))
                restartJob = null
                processingJob = null
                maintenanceJob = null
            }
            Log.trace(logger, project, "stopJobs completed")
        }
    }


    private fun restartJobsWithDelay() {
        Log.trace(logger, project, "restartJobsWithDelay called")
        restartJob?.cancel()
        restartJob = cs.launchWithErrorReporting("${this::class.java.simpleName}.RestartJobsWithDelay", logger) {
            myStopStartMutex.withLock {
                delay(10.seconds)
                coroutineContext.ensureActive()
                //always wait for smart mode to launch the jobs
                @Suppress("UnstableApiUsage")
                project.waitForSmartMode()
                coroutineContext.ensureActive()
                Log.trace(logger, project, "restarting after delay")
                if (!startupJobCompleted.get()) {
                    launchStartupJob()
                }
                launchCandidatesProcessingJob()
                launchMaintenanceJob()
                Log.trace(logger, project, "restartJobsWithDelay completed")
            }
        }
    }


    override fun dispose() {
        Log.trace(logger, project, "disposing")
        statusJob?.cancel()
        candidateFiles.clear()
        jonManager.dispose()
    }


    private fun launchStartupJob() {
        if (startupJob?.isActive == true || startupJobCompleted.get()) {
            return
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
                                addCandidateFile(candidateFile)
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
            if (throwable != null && throwable !is CancellationException) {
                Log.warnWithException(logger, project, throwable, "Error while running startup job")
                ErrorReporter.getInstance().reportError("AbstractNavigationDiscoveryManager.launchStartupJob", throwable)
            }
        }
    }


    private fun addCandidateFile(file: VirtualFile) {
        //This method is called from the index when a file is indexed. Or from the startup job.
        //Don't add it to candidateFiles immediately, give some time for the indexing process to complete.
        //Our index may be ready, but maybe the intellij stub index is not completed for that file.
        //This is a defensive action against indexing errors like an outdated file in index or psi stub mismatch.
        cs.launchWithErrorReporting("${this::class.java.simpleName}.addCandidateFile", logger) {
            delay(5.seconds)
            if (isProjectValid(project) && isValidVirtualFile(file)) {
                Log.trace(logger, project, "adding candidate file to candidateFiles {}", file.url)
                candidateFiles.offer(file)
            }
        }
    }


    override fun fileUpdated(file: VirtualFile, keys: List<String>) {
        Log.trace(logger, project, "fileUpdated for file {} with keys {}", file.url, keys)
        addCandidateFile(file)
    }


    private fun launchCandidatesProcessingJob() {
        if (processingJob?.isActive == true) {
            return
        }
        Log.trace(logger, project, "launching candidate processing job")
        processingJob?.cancel(CancellationException("new job started"))
        processingJob = cs.launchWhileActiveWithErrorReporting(10.seconds, 30.seconds, "${this::class.java.simpleName}.ProcessingTask", logger) {
            coroutineContext.ensureActive()
            @Suppress("UnstableApiUsage")
            project.waitForSmartMode()
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


                //Run the discovery under file monitoring that will cancel the discovery if the file changes while discovering.
                //The jobs that FileProcessingMonitor starts are child coroutines of this coroutine. They will be canceled when
                // this coroutine, the processingJob, is canceled.
                val fileInfoResult = FileProcessingMonitor(project, logger).executeWithFileMonitoring(file) {
                    Log.trace(logger, project, "Processing candidate file {}", file.url)
                    @Suppress("UnstableApiUsage")
                    project.waitForSmartMode()
                    val fileInfoWithTime = measureTimeMillisWithResult {
                        coroutineContext.ensureActive()
                        FileDiscoveryInfoBuilder.getInstance(project).buildFileInfo(file)
                    }
                    Log.trace(logger, project, "Finished processing candidate file {} in {} ms", file.url, fileInfoWithTime.second)
                    fileInfoWithTime.first
                }

                coroutineContext.ensureActive()

                when (fileInfoResult) {
                    is ProcessingResult.Success -> {
                        Log.trace(logger, project, "Built fileInfo for candidate file {}", file.url)
                        processFileInfo(fileInfoResult.result)
                        candidateFiles.remove(file)
                    }
                    //If there was an error in discovery, remove the file. If it's changed again, it will be added again.
                    is ProcessingResult.Error -> {
                        if (fileInfoResult.exception == null) {
                            Log.warn(logger, project, "Error while processing candidate file {}: {}", file.url, fileInfoResult.message)
                        } else {
                            Log.warnWithException(
                                logger,
                                project,
                                fileInfoResult.exception,
                                "Error while processing candidate file {}: {}",
                                file.url,
                                fileInfoResult.message
                            )
                        }

                        val errorsCount = discoveryErrorFiles.getOrPut(file.url) { 0 } + 1
                        discoveryErrorFiles[file.url] = errorsCount
                        candidateFiles.remove(file)
                        //allow 5 errors for a file. don't try to discover again after that
                        if (errorsCount <= 5) {
                            addCandidateFile(file)
                        } else {
                            Log.trace(logger, project, "Too many errors for file {}. Not trying again.", file.url)
                        }

                    }
                    //If the discovery was canceled because the file changed while discovery is running, remove it from candidate files.
                    // And add it back to the end of the queue using addCandidateFile to let some time for indexing to complete and process it again.
                    is ProcessingResult.Cancelled -> {
                        Log.trace(logger, project, "Processing canceled for candidate file {}: {}", file.url, fileInfoResult.reason)
                        candidateFiles.remove(file)
                        addCandidateFile(file)
                    }
                }

                file = candidateFiles.peek()
            }
        }
    }


    private fun launchMaintenanceJob() {
        if (maintenanceJob?.isActive == true) {
            return
        }
        Log.trace(logger, project, "launching maintenance job")
        maintenanceJob = cs.launchWhileActiveWithErrorReporting(1.minutes, 1.minutes, "${this::class.java.simpleName}.MaintenanceTask", logger) {
            coroutineContext.ensureActive()
            @Suppress("UnstableApiUsage")
            project.waitForSmartMode()
            coroutineContext.ensureActive()
            maintenance()
        }
    }


    fun launchStatusJob() {
        statusJob = cs.launchWhileActiveWithErrorReporting(1.minutes, 1.minutes, "${this::class.java.simpleName}.StatusTask", logger) {
            val discoveryStatus = getDiscoveryStatus().replace("\n", "; ")
            val status = "${this@AbstractNavigationDiscoveryManager::class.simpleName} Status: project=${project.name}," +
                    "startupJob=${startupJob?.isActive}, startupJob finished: ${startupJobCompleted.get()}, " +
                    "processingJob=${processingJob?.isActive}, " +
                    "maintenanceJob=${maintenanceJob?.isActive}, " +
                    "restartJob=${restartJob?.isActive}, " +
                    "candidateFiles=${candidateFiles.size}, " +
                    "discoveryErrors=${discoveryErrorFiles.size}, " +
                    "JobManagerStatus=[${jonManager.status()}], " +
                    "DiscoveryStatus=[${discoveryStatus.trim()}]"


            Log.trace(logger, project, status)
        }
    }

}