package org.digma.intellij.plugin.idea.navigation

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeAnyChangeAbstractAdapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.common.isValidVirtualFile
import org.digma.intellij.plugin.common.runInReadAccessWithResult
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.psi.isJvmSupportedFile
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.psi.PsiUtils
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.min
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * This service listens to source code changes and notifies the navigation discovery services about changes.
 * It uses a combination of PsiTreeChangeListener and AsyncFileListener.
 * PsiTreeChangeListener notifies immediately about psi changes but doesn't notify about deletion, move and copy.
 * AsyncFileListener notifies about deletion, move and copy, but may take long until it notifies about psi changes.
 * with these two listeners we are notifies of all changes. it may happen that the same file will be processed more
 * than once for the same change.
 */
@Service(Service.Level.PROJECT)
class NavigationDiscoveryChangeService(private val project: Project, private val cs: CoroutineScope) : Disposable {

    private val logger = Logger.getInstance(this::class.java)

    private val quitePeriod = 5.seconds

    //using LinkedHashSet to keep insertion order so we process files in the order they were changed.
    //there is only write operation to this set, never read or remove. before processing, the set is replaced with a new instance
    // and processing runs on the old set so we don't need to wary about concurrent modifications.
    //we keep the url for virtual files and not the virtual file itself, first reason is that we don't know if all virtual file classes
    // implement hashCode and equals because we don't want duplicate entries for the same file. second reason is not to keep references
    // to virtual files for too long.
    private var changedFiles = createChangedFilesSet()

    //using LinkedList to keep insertion order, no need for a set because VFileEvent are never equals.
    //there is only write operation to this list, never read or remove. before processing, the list is replaced with a new instance
    // and processing runs on the old instance.
    private var bulkEvents = createBulkEventsList()

    private val changedFilesBulks: Queue<Iterable<String>> = ConcurrentLinkedQueue()
    private val bulkEventsBulks: Queue<Iterable<VFileEvent>> = ConcurrentLinkedQueue()

    private var lastChangedFileEventTime: Instant? = null
    private var lastBulkFileChangeEventTime: Instant? = null

    private val changeFilesProcessingJob: Job = launchBulkProcessingLoop(changedFilesBulks, ChangedFileProcessor())
    private val bulkEventsProcessingJob: Job = launchBulkProcessingLoop(bulkEventsBulks, FileEventsProcessor())
    private val quitePeriodManagerJob: Job = launchQuitePeriodManager()
    private var launchFullUpdateJob: Job? = null

    private val paused = AtomicBoolean(false)

    private val launchFullUpdateLock = ReentrantLock(true)


    companion object {
        fun createChangedFilesSet(): MutableSet<String> {
            return LinkedHashSet()
        }

        fun createBulkEventsList(): MutableList<VFileEvent> {
            return LinkedList<VFileEvent>()
        }
    }


    init {
        PsiManager.getInstance(project).addPsiTreeChangeListener(NavigationDiscoveryPsiTreeChangeListener(), this)
        VirtualFileManager.getInstance().addAsyncFileListener(NavigationDiscoveryAsyncFileChangeListener(), this)
    }


    override fun dispose() {
        changeFilesProcessingJob.cancel()
        bulkEventsProcessingJob.cancel()
        quitePeriodManagerJob.cancel()
    }


    private fun pause() {
        Log.log(logger::trace, project, "pausing")
        paused.set(true)
    }

    private fun resume() {
        Log.log(logger::trace, project, "resuming")
        paused.set(false)
    }

    private fun isPaused(): Boolean {
        return paused.get()
    }


    private fun launchQuitePeriodManager(): Job {
        return cs.launch {

            Log.log(logger::trace, project, "Starting quite period manager")

            while (isActive) {

                delay(200)

                try {

                    if (changedFiles.isNotEmpty()) {
                        val now = Clock.System.now()
                        val quitePeriodFileChange = lastChangedFileEventTime?.let {
                            now - it
                        } ?: ZERO
                        if (quitePeriodFileChange >= quitePeriod) {
                            val myChangedFiles = changedFiles
                            changedFiles = createChangedFilesSet()
                            changedFilesBulks.add(myChangedFiles)
                        }
                    }

                    if (bulkEvents.isNotEmpty()) {
                        val now = Clock.System.now()
                        val quitePeriodBulkFileEvent = lastBulkFileChangeEventTime?.let {
                            now - it
                        } ?: ZERO
                        if (quitePeriodBulkFileEvent >= quitePeriod) {
                            val myBulkEvents = bulkEvents
                            bulkEvents = createBulkEventsList()
                            bulkEventsBulks.add(myBulkEvents)
                        }
                    }

                } catch (e: Throwable) {
                    Log.warnWithException(logger, project, e, "Exception in NavigationDiscoveryChangeService.launchQuitePeriodManager {}", e)
                    ErrorReporter.getInstance().reportError(project, "NavigationDiscoveryChangeService.launchQuitePeriodManager", e)
                }
            }

            Log.log(logger::trace, project, "quite period manager exited")
        }
    }


    private fun <T> launchBulkProcessingLoop(queue: Queue<Iterable<T>>, itemProcessor: ItemProcessor<T>): Job {
        return cs.launch {
            Log.log(logger::trace, project, "Starting bulk processing loop for {}", itemProcessor::class.java)
            while (isActive && isProjectValid(project)) {

                try {

                    val bulk = queue.poll()
                    if (bulk == null) {
                        delay(5.seconds)
                    } else {
                        bulk.forEach {
                            if (!isActive) {
                                // Cancelled, stop processing
                                return@forEach
                            }
                            itemProcessor.process(it)
                        }
                    }

                } catch (ce: CancellationException) {
                    throw ce
                } catch (e: Throwable) {
                    Log.warnWithException(logger, project, e, "Exception in NavigationDiscoveryChangeService.launchBulkProcessingLoop {}", e)
                    ErrorReporter.getInstance().reportError(project, "NavigationDiscoveryChangeService.launchBulkProcessingLoop", e)
                }
            }

            Log.log(logger::trace, project, "Bulk processing loop exited for {}", itemProcessor::class.java)
        }
    }


    /*
    sometimes there are large updates in a project. large updates may happen when changing branch in a large project
    or when doing a refactor that impacts many files.
    when that happens it doesn't make sense to update the files one by one, it will take too long, the changedFiles and
    bulkEvents lists will grow too large. and the executor service queue of the navigation discovery will grow too large.
    we see sometimes updates of thousands of files. one example if to open the intellij-community repository and change branch,
    it will trigger an update of more than 40000 files. and we see also users with this amount of updates.

    when we discover a large update we want to:
    pause collecting events and clean the currently collected events.
    wait for a quite period of no events, 10 seconds seems reasonable
    launch a full discovery update
    resume collecting events

     */
    //this function should be very fast , it is called on EDT
    private fun pauseAndLaunchFullUpdate() {

        Log.log(logger::trace, project, "pauseAndLaunchFullUpdate was called")

        if (launchFullUpdateJob?.isActive == true) {
            Log.log(logger::trace, project, "pauseAndLaunchFullUpdate was called but launchFullUpdateJob is already active, aborting")
            return
        }

        //make sure not to launch twice because events keep coming until pause is called
        launchFullUpdateLock.withLock {

            if (launchFullUpdateJob?.isActive == true) {
                Log.log(logger::trace, project, "pauseAndLaunchFullUpdate was called but launchFullUpdateJob is already active, aborting")
                return
            }

            Log.log(logger::trace, project, "launching launchFullUpdateJob")
            launchFullUpdateJob = cs.launch {

                try {

                    Log.log(logger::trace, project, "launchFullUpdateJob started")

                    //pause and clear all collected events so no more update tasks will run until resume
                    Log.log(logger::trace, project, "pausing and clearing all collected events in launchFullUpdateJob")
                    pause()
                    //replace the lists , never call clear or remove to avoid concurrent modifications
                    changedFiles = createChangedFilesSet()
                    bulkEvents = createBulkEventsList()
                    //It's ok to call clear on the queues because we use concurrent queue
                    changedFilesBulks.clear()
                    bulkEventsBulks.clear()

                    //wait for a quite period of no change events.
                    //during this time the update tasks that already started will probably finish
                    Log.log(logger::trace, project, "waiting for quite period in launchFullUpdateJob")
                    var quitePeriod = ZERO
                    while (isActive && quitePeriod < 10.seconds) {

                        Log.log(logger::trace, project, "checking quite period in launchFullUpdateJob, current quite period {}", quitePeriod)

                        val now = Clock.System.now()
                        val quitePeriodChangedFile = lastChangedFileEventTime?.let {
                            now - it
                        } ?: ZERO

                        val quitePeriodBulkFileEvent = lastBulkFileChangeEventTime?.let {
                            now - it
                        } ?: ZERO


                        //maybe there was only psi tree change events or only bulk change events.
                        //if there was both, wait for quite period for both, if there was only one, wait for quite period
                        // only for this one
                        quitePeriod = if (lastChangedFileEventTime != null && lastBulkFileChangeEventTime != null) {
                            min(
                                quitePeriodChangedFile.inWholeMilliseconds,
                                quitePeriodBulkFileEvent.inWholeMilliseconds
                            ).toDuration(DurationUnit.MILLISECONDS)
                        } else if (lastChangedFileEventTime != null) {
                            quitePeriodChangedFile
                        } else {
                            quitePeriodBulkFileEvent
                        }

                        delay(1000)
                    }
                } catch (e: Throwable) {
                    //there should not be an exception here, the code in the try block should not produce any error.
                    // but protect against who knows what. we must make sure that resume is called eventually.
                    Log.warnWithException(logger, project, e, "error in pauseAndLaunchFullUpdate {}", e)
                    ErrorReporter.getInstance().reportError("NavigationDiscoveryChangeService.pauseAndLaunchFullUpdate", e)
                }


                Log.log(logger::trace, project, "quite period completed in launchFullUpdateJob, launching full update")
                //what ever happens resume must be called before this coroutine completes
                try {
                    //if the coroutine was canceled don't call the update, probably the project was closed
                    if (isActive) {
                        Log.log(logger::info, project, "launching full update span discovery in launchFullUpdateJob")
                        val jvmSpanNavigationProvider = JvmSpanNavigationProvider.getInstance(project)
                        jvmSpanNavigationProvider.buildNavigationDiscoveryFullUpdate()

                        Log.log(logger::info, project, "launching full update endpoint discovery in launchFullUpdateJob")
                        val javaEndpointNavigationProvider = JvmEndpointNavigationProvider.getInstance(project)
                        javaEndpointNavigationProvider.buildNavigationDiscoveryFullUpdate()
                    }
                } catch (e: Throwable) {
                    Log.warnWithException(logger, project, e, "error launching full update {}", e)
                    ErrorReporter.getInstance().reportError("NavigationDiscoveryChangeService.pauseAndLaunchFullUpdate", e)
                } finally {
                    resume()
                }
            }
        }
    }


    private fun addChangedFile(virtualFile: VirtualFile) {

        lastChangedFileEventTime = Clock.System.now()

        if (isPaused()) {
            return
        }

        //protect against high memory consumption, if so many files are changed at once pause collecting changed files and launch full update
        if (changedFiles.size > 200) {

            Log.log(logger::trace, project, "discovered too many changed files {}", changedFiles.size)

            pauseAndLaunchFullUpdate()

            ErrorReporter.getInstance().reportErrorSkipFrequencyCheck(
                project,
                "NavigationDiscoveryChangeService.addChangedFile", "too many changed files", mapOf(
                    "changedFiles.size" to changedFiles.size
                )
            )
            return
        }
        changedFiles.add(virtualFile.url)

    }


    private inner class NavigationDiscoveryPsiTreeChangeListener : PsiTreeAnyChangeAbstractAdapter() {
        override fun onChange(file: PsiFile?) {

            try {
                if (!isProjectValid(project)) {
                    return
                }

                //this method should be very fast, it runs on EDT.
                // selecting relevant files should be done while processing in background.
                file?.let {
                    //very quick selection of java and kotlin files only.
                    //when the files are processed there is another check isJvmSupportedFile, but instead of collecting
                    // all files and checking later we check here first because there may be many files that are not relevant
                    if (isJavaOrKotlinFile(it)) {
                        addChangedFile(it.virtualFile)
                    }
                }
            } catch (e: Throwable) {
                ErrorReporter.getInstance().reportError("NavigationDiscoveryChangeService.NavigationDiscoveryPsiTreeChangeListener.onChange", e)
            }
        }
    }

    private fun addBulkEvents(events: List<VFileEvent>) {

        lastBulkFileChangeEventTime = Clock.System.now()

        if (isPaused()) {
            return
        }

        //protect against high memory consumption, if so many files are changed at once pause collecting events and launch a full update
        if (bulkEvents.size > 200) {

            Log.log(logger::trace, project, "discovered too many bulk change events {}", bulkEvents.size)

            pauseAndLaunchFullUpdate()

            ErrorReporter.getInstance().reportErrorSkipFrequencyCheck(
                project,
                "NavigationDiscoveryChangeService.addBulkEvent", "too many bulk change events", mapOf(
                    "bulkEvents.size" to bulkEvents.size
                )
            )
            return
        }
        bulkEvents.addAll(events)

    }


    private inner class NavigationDiscoveryAsyncFileChangeListener : AsyncFileListener {
        override fun prepareChange(events: List<VFileEvent>): AsyncFileListener.ChangeApplier? {

            try {
                if (!isProjectValid(project)) {
                    return null
                }

                return object : AsyncFileListener.ChangeApplier {
                    override fun afterVfsChange() {
                        //this method should be very fast, it runs with write action.
                        //selecting relevant files should be done while processing in background.
                        try {
                            addBulkEvents(events)
                        } catch (e: Throwable) {
                            ErrorReporter.getInstance()
                                .reportError("NavigationDiscoveryChangeService.NavigationDiscoveryAsyncFileChangeListener.afterVfsChange", e)
                        }

                    }
                }
            } catch (e: Throwable) {
                ErrorReporter.getInstance()
                    .reportError("NavigationDiscoveryChangeService.NavigationDiscoveryAsyncFileChangeListener.prepareChange", e)
                return null
            }
        }
    }


    private interface ItemProcessor<T> {
        fun process(item: T)
    }


    private inner class FileEventsProcessor : ItemProcessor<VFileEvent> {
        override fun process(item: VFileEvent) {

            try {

                if (isRelevantFile(project, item.file)) {

                    //run a quick check if the event is for a java or kotlin file and abort processing if not.
                    //if we can't find PsiFile continue processing, maybe it's a copy or delete or other event that we want to process
                    item.file?.let { vf ->
                        val psiFile = findPsiFileForVirtualFile(vf)
                        psiFile?.let { psf ->
                            if (!isJavaOrKotlinFile(psf)) {
                                return
                            }
                        }
                    }

                    Log.log(logger::trace, project, "processing file event {}", item)

                    when (item) {
                        is VFileDeleteEvent -> {
                            if (isJavaOrKotlinFile(item.file)) {
                                deleteFromNavigation(project, item.file)
                            }
                        }

                        is VFilePropertyChangeEvent -> {
                            //we're interested only when the name property is changed
                            if (item.propertyName == VirtualFile.PROP_NAME) {
                                if (isJavaOrKotlinFile(item.file)) {
                                    deleteFromNavigationByOldPath(project, item.oldPath)
                                    addChangedFile(item.file)
                                }
                            }
                        }

                        is VFileMoveEvent -> {
                            if (isJavaOrKotlinFile(item.file)) {
                                deleteFromNavigationByOldPath(project, item.oldPath)
                                addChangedFile(item.file)
                            }
                        }

                        is VFileCopyEvent -> {
                            item.findCreatedFile()?.let { newFile ->
                                if (isJavaOrKotlinFile(newFile)) {
                                    addChangedFile(newFile)
                                }
                            }
                        }

                        else -> {
                            item.file?.let {
                                if (isJavaOrKotlinFile(it)) {
                                    addChangedFile(it)
                                }
                            }
                        }
                    }
                }

            } catch (e: Throwable) {
                Log.warnWithException(logger, project, e, "Exception in FileEventsProcessor.process for {}", item)
                ErrorReporter.getInstance().reportError(project, "NavigationDiscoveryChangeService.FileEventsProcessor.process", e, mapOf())
            }
        }
    }


    private inner class ChangedFileProcessor : ItemProcessor<String> {
        override fun process(item: String) {

            try {

                Log.log(logger::trace, project, "processing changed file {}", item)

                val virtualFile = VirtualFileManager.getInstance().findFileByUrl(item)
                virtualFile?.let {
                    updateNavigation(project, it)
                }
            } catch (e: Throwable) {
                Log.warnWithException(logger, project, e, "Exception in ChangedFileProcessor.process for {}", item)
                ErrorReporter.getInstance().reportError(project, "NavigationDiscoveryChangeService.ChangedFileProcessor.process", e, mapOf())
            }
        }
    }


    private fun updateNavigation(project: Project, file: VirtualFile) {
        if (isValidRelevantFile(project, file)) {
            val psiFile = runInReadAccessWithResult {
                PsiManager.getInstance(project).findFile(file)
            }
            psiFile?.let {
                if (PsiUtils.isValidPsiFile(it) && isJvmSupportedFile(project, it)) {
                    Log.log(logger::trace, project, "calling fileChanged for {}", file)
                    JvmSpanNavigationProvider.getInstance(project).fileChanged(file)
                    JvmEndpointNavigationProvider.getInstance(project).fileChanged(file)
                }
            }
        }
    }


    private fun deleteFromNavigation(project: Project, file: VirtualFile) {
        //deleted file may be any file. a java/kotlin file will not be in content anymore and will be invalid but
        // will still have a path, so we can remove it from discovery.
        //primitive check that it's a java or kotlin file.
        //better not to call file deleted if it's not a relevant file although nothing will happen if the file was not mapped.
        //a deleted file doesn't have PsiFile anymore so we can't find PsiFile and check language.
        if (isJavaOrKotlinFile(file)) {
            Log.log(logger::trace, project, "calling fileDeleted for {}", file)
            JvmSpanNavigationProvider.getInstance(project).fileDeleted(file)
            JvmEndpointNavigationProvider.getInstance(project).fileDeleted(file)
        }
    }

    private fun deleteFromNavigationByOldPath(project: Project, oldPath: String) {
        //primitive check that it's a java or kotlin file.
        //better not to call file deleted if it's not a relevant file although nothing will happen if the file was not mapped.
        //a deleted file doesn't have PsiFile anymore so we can't find PsiFile and check language.
        if (isJavaOrKotlinPath(oldPath)) {
            Log.log(logger::trace, project, "calling pathDeleted for {}", oldPath)
            JvmSpanNavigationProvider.getInstance(project).pathDeleted(oldPath)
            JvmEndpointNavigationProvider.getInstance(project).pathDeleted(oldPath)
        }
    }


    private fun findPsiFileForVirtualFile(virtualFile: VirtualFile): PsiFile? {
        return try {
            runInReadAccessWithResult {
                virtualFile.takeIf { isValidVirtualFile(virtualFile) }?.let {
                    PsiManager.getInstance(project).findFile(it)
                }
            }
        } catch (e: Throwable) {
            null
        }
    }

    private fun isJavaOrKotlinFile(psiFile: PsiFile): Boolean {
        return psiFile.language.displayName.equals("Java", true) ||
                psiFile.language.displayName.equals("Kotlin", true)
    }

    private fun isJavaOrKotlinFile(virtualFile: VirtualFile): Boolean {
        return virtualFile.name.endsWith(".java", true) ||
                virtualFile.name.endsWith(".kt", true)
    }

    private fun isJavaOrKotlinPath(path: String): Boolean {
        return path.endsWith(".java", true) ||
                path.endsWith(".kt", true)
    }

}






