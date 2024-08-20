package org.digma.intellij.plugin.idea.navigation

import com.intellij.lang.java.JavaLanguage
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
import org.jetbrains.kotlin.idea.KotlinLanguage
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds

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
    //there is only write operation to this set, never read or remove. before processing the set is replaced with a new instance
    // and processing runs on the old set so we don't need to wary about concurrent modifications.
    //we keep the url for virtual files and not the virtual file itself, first reason is we don't know if all virtual file classes
    // implement hashCode and equals because we don't want duplicate entries for the same file. second reason is not to keep references
    // to virtual files for too long.
    private var changedFiles = LinkedHashSet<String>()

    //using LinkedList to keep insertion order, no need for a set because VFileEvent are never equals.
    //there is only write operation to this list, never read or remove. before processing the list is replaced with a new instance.
    private var bulkEvents = LinkedList<VFileEvent>()

    private val changedFilesBulks: Queue<Iterable<String>> = ConcurrentLinkedQueue()
    private val bulkEventsBulks: Queue<Iterable<VFileEvent>> = ConcurrentLinkedQueue()

    private var lastChangedFileEventTime: Instant? = null
    private var lastBulkFileChangeEventTime: Instant? = null

    private val changeFilesProcessingJob: Job = launchBulkProcessingLoop(changedFilesBulks, ChangedFileProcessor())
    private val bulkEventsProcessingJob: Job = launchBulkProcessingLoop(bulkEventsBulks, FileEventsProcessor())
    private val quitePeriodManagerJob: Job = launchQuitePeriodManager()

    init {
        PsiManager.getInstance(project).addPsiTreeChangeListener(NavigationDiscoveryPsiTreeChangeListener(), this)
        VirtualFileManager.getInstance().addAsyncFileListener(NavigationDiscoveryAsyncFileChangeListener(), this)
    }


    override fun dispose() {
        changeFilesProcessingJob.cancel()
        bulkEventsProcessingJob.cancel()
        quitePeriodManagerJob.cancel()
    }


    private fun launchQuitePeriodManager(): Job {
        return cs.launch {

            Log.log(logger::trace, "Starting quite period manager")

            while (isActive) {

                delay(200)

                try {

                    if (changedFiles.isNotEmpty()) {
                        val now = Clock.System.now()
                        val quiteFileChangePeriod = lastChangedFileEventTime?.let {
                            now - it
                        } ?: ZERO
                        if (quiteFileChangePeriod >= quitePeriod) {
                            val myChangedFiles = changedFiles
                            changedFiles = LinkedHashSet()
                            changedFilesBulks.add(myChangedFiles)
                        }
                    }

                    if (bulkEvents.isNotEmpty()) {
                        val now = Clock.System.now()
                        val quiteBulkFileEventPeriod = lastBulkFileChangeEventTime?.let {
                            now - it
                        } ?: ZERO
                        if (quiteBulkFileEventPeriod >= quitePeriod) {
                            val myBulkEvents = bulkEvents
                            bulkEvents = LinkedList()
                            bulkEventsBulks.add(myBulkEvents)
                        }
                    }

                } catch (e: Throwable) {
                    Log.warnWithException(logger, e, "Exception in NavigationDiscoveryChangeService.launchQuitePeriodManager {}", e)
                    ErrorReporter.getInstance().reportError(project, "NavigationDiscoveryChangeService.launchQuitePeriodManager", e)
                }
            }

            Log.log(logger::trace, "quite period manager exited")
        }
    }


    private fun <T> launchBulkProcessingLoop(queue: Queue<Iterable<T>>, itemProcessor: ItemProcessor<T>): Job {
        return cs.launch {
            Log.log(logger::trace, "Starting bulk processing loop for {}", itemProcessor::class.java)
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
                    Log.warnWithException(logger, e, "Exception in NavigationDiscoveryChangeService.launchBulkProcessingLoop {}", e)
                    ErrorReporter.getInstance().reportError(project, "NavigationDiscoveryChangeService.launchBulkProcessingLoop", e)
                }
            }

            Log.log(logger::trace, "Bulk processing loop exited for {}", itemProcessor::class.java)
        }
    }


    private fun addChangedFile(virtualFile: VirtualFile) {
        //protect against high memory consumption, if so many files are changed at once the navigation will not be updated.
        //report an error so we know about it
        if (changedFiles.size > 10000) {
            ErrorReporter.getInstance().reportError(
                "NavigationDiscoveryChangeService.addChangedFile", "too many changed files", mapOf(
                    "changedFiles.size" to changedFiles.size
                )
            )
            return
        }
        changedFiles.add(virtualFile.url)
        lastChangedFileEventTime = Clock.System.now()
    }


    private inner class NavigationDiscoveryPsiTreeChangeListener : PsiTreeAnyChangeAbstractAdapter() {
        override fun onChange(file: PsiFile?) {

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
        }
    }

    private fun addBulkEvents(events: List<VFileEvent>) {
        //protect against high memory consumption, if so many files are changed at once the navigation will not be updated.
        //report an error so we know about it
        if (bulkEvents.size > 10000) {
            ErrorReporter.getInstance().reportError(
                "NavigationDiscoveryChangeService.addBulkEvent", "too many bulk change events", mapOf(
                    "bulkEvents.size" to bulkEvents.size
                )
            )
            return
        }
        bulkEvents.addAll(events)
        lastBulkFileChangeEventTime = Clock.System.now()
    }


    private inner class NavigationDiscoveryAsyncFileChangeListener : AsyncFileListener {
        override fun prepareChange(events: List<VFileEvent>): AsyncFileListener.ChangeApplier? {

            if (!isProjectValid(project)) {
                return null
            }

            return object : AsyncFileListener.ChangeApplier {
                override fun afterVfsChange() {
                    //this method should be very fast, it runs with write action.
                    //selecting relevant files should be done while processing in background.
                    addBulkEvents(events)
                }
            }
        }
    }


    private interface ItemProcessor<T> {
        fun process(item: T)
    }


    private inner class FileEventsProcessor : ItemProcessor<VFileEvent> {
        override fun process(item: VFileEvent) {

            try {

                Log.log(logger::trace, "processing file event {}", item)

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

                    when (item) {
                        is VFileDeleteEvent -> {
                            deleteFromNavigation(project, item.file)
                        }

                        is VFilePropertyChangeEvent -> {
                            //we're interested only when the name property is changed
                            if (item.propertyName == VirtualFile.PROP_NAME) {
                                deleteFromNavigationByOldPath(project, item.oldPath)
                                addChangedFile(item.file)
                            }
                        }

                        is VFileMoveEvent -> {
                            deleteFromNavigationByOldPath(project, item.oldPath)
                            addChangedFile(item.file)
                        }

                        is VFileCopyEvent -> {
                            item.findCreatedFile()?.let { newFile ->
                                addChangedFile(newFile)
                            }
                        }

                        else -> {
                            item.file?.let {
                                addChangedFile(it)
                            }
                        }
                    }
                }

            } catch (e: Throwable) {
                Log.warnWithException(logger, e, "Exception in FileEventsProcessor.process for {}", item)
                ErrorReporter.getInstance().reportError(project, "NavigationDiscoveryChangeService.FileEventsProcessor.process", e, mapOf())
            }
        }
    }


    private inner class ChangedFileProcessor : ItemProcessor<String> {
        override fun process(item: String) {

            try {

                Log.log(logger::trace, "processing changed file {}", item)

                val virtualFile = VirtualFileManager.getInstance().findFileByUrl(item)
                virtualFile?.let {
                    updateNavigation(project, it)
                }
            } catch (e: Throwable) {
                Log.warnWithException(logger, e, "Exception in ChangedFileProcessor.process for {}", item)
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
                    Log.log(logger::trace, "calling fileChanged for {}", file)
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
        if (file.name.endsWith("java", true) || file.name.endsWith("kt", true)) {
            Log.log(logger::trace, "calling fileDeleted for {}", file)
            JvmSpanNavigationProvider.getInstance(project).fileDeleted(file)
            JvmEndpointNavigationProvider.getInstance(project).fileDeleted(file)
        }
    }

    private fun deleteFromNavigationByOldPath(project: Project, oldPath: String) {
        //primitive check that it's a java or kotlin file.
        //better not to call file deleted if it's not a relevant file although nothing will happen if the file was not mapped.
        //a deleted file doesn't have PsiFile anymore so we can't find PsiFile and check language.
        if (oldPath.endsWith("java", true) || oldPath.endsWith("kt", true)) {
            Log.log(logger::trace, "calling pathDeleted for {}", oldPath)
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
        return psiFile.language == JavaLanguage.INSTANCE || psiFile.language == KotlinLanguage.INSTANCE
    }

}






