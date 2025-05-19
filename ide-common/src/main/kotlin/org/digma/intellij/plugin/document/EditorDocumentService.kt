package org.digma.intellij.plugin.document

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.digma.intellij.plugin.common.FileUtils
import org.digma.intellij.plugin.common.isValidVirtualFile
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.psi.LanguageServiceProvider
import org.digma.intellij.plugin.psi.isSupportedLanguageFile
import java.time.Instant
import java.util.Queue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * handles open documents in the IDE editor.
 * builds DocumentInfo when files are opened and updated when documents change, and removes DocumentInfo when files are closed.
 */
@Service(Service.Level.PROJECT)
class EditorDocumentService(private val project: Project, private val cs: CoroutineScope) : Disposable {

    private val logger = Logger.getInstance(this::class.java)

    companion object {
        @JvmStatic
        fun getInstance(project: Project): EditorDocumentService {
            return project.service<EditorDocumentService>()
        }
    }

    private val quitePeriodForUpdateSeconds = 10.seconds.toJavaDuration()
    private var lastUpdateTimestamps: Instant? = null
    private val runningJobs: MutableMap<VirtualFile, Job> = ConcurrentHashMap()
    private val putRemoveLock = Mutex()
    private val changedDocuments: Queue<Document> = ConcurrentLinkedQueue()
    private val filesToUpdate: MutableSet<VirtualFile> = ConcurrentHashMap.newKeySet()
    private val documentChangeListenerDisposables: MutableMap<VirtualFile, Disposable> = ConcurrentHashMap()
    private val myDocumentListener = MyDocumentChangeListener()

    init {
        cs.launch {
            //runs for the lifetime of the project
            while (isActive) {
                try {
                    delay(1.seconds)
                    processDocumentQueue()
                } catch (e: CancellationException) {
                    throw e // ⚠️ Always rethrow to propagate cancellation properly
                } catch (e: Throwable) {
                    Log.warnWithException(logger, e, "Exception in EditorDocumentService.processDocumentQueue {}", e)
                    ErrorReporter.getInstance().reportError(project, "EditorDocumentService.processDocumentQueue", e)
                }
            }
        }
    }

    private inner class MyDocumentChangeListener() : DocumentListener {
        override fun documentChanged(event: DocumentEvent) {

            /*
              The central concern in updates is to do as little as possible on EDT and to not take read access too much.
              Updating the document info happens only after a quiet period of no updates, actually when the user stops typing.
              While waiting for quite period we don't run anything on EDT and don't take read access.
           */

            try {
                //update the timestamp before adding the document
                lastUpdateTimestamps = Instant.now()
                //This code runs on EDT almost for every keystroke, so we need to be very fast here.
                //It's a raw collection of changed documents, this is the fastest thing we can do.
                //Putting the document in a queue is very fast, putting it in a set or converting to VirtualFile takes longer.
                //It could be easier to use VirtualFile, but that will put a load on EDT for every keystroke.
                changedDocuments.add(event.document)
            } catch (e: Throwable) {
                Log.warnWithException(logger, e, "Exception in MyDocumentChangeListener.documentChanged {}", e)
                ErrorReporter.getInstance().reportError(project, "EditorDocumentService.MyDocumentChangeListener.documentChanged", e)
            }
        }
    }


    //this method is called on EDT and must be fast
    fun fileOpened(file: VirtualFile) {
        try {
            doOnFileOpened(file)
        }catch (e:Throwable){
            Log.warnWithException(logger,e,"Exception in fileOpened {}",e)
            ErrorReporter.getInstance().reportError(project, "EditorDocumentService.fileOpened", e)
        }
    }

    private fun doOnFileOpened(file: VirtualFile) {

        //usually we are not interested in non-writable files, they are library sources or vcs files etc.
        //although a user may mark a content file as non-writable, but this is probably very rare.
        if (!file.isWritable) {
            return
        }

        //Register document change listener for every opened document.
        //Note: I tried to use EditorFactory.getInstance().eventMulticaster but it will fire hundreds of events for every file open file also for non-writable
        // files. Don't use it!
        if (isRelevantLanguageFileNoSlowOperations(file)) {

            if (logger.isTraceEnabled) {
                Log.log(logger::trace, "fileOpened: processing {}", file)
            }

            //this is protection against dev error, it should never happen. we should catch it in development.
            if (documentChangeListenerDisposables.contains(file)) {
                throw IllegalStateException("fileOpened: document change listener already registered for file $file")
            }


            val document = FileDocumentManager.getInstance().getDocument(file)
            if (document != null) {
                if (logger.isTraceEnabled) {
                    Log.log(logger::trace, "fileOpened: adding document listener for {}", file)
                }
                val disposable = Disposer.newDisposable()
                document.addDocumentListener(myDocumentListener, disposable)
                documentChangeListenerDisposables[file] = disposable
                if (logger.isTraceEnabled) {
                    Log.log(logger::trace, "fileOpened: starting build document info job for {}", file)
                }
                launchBuildDocumentInfoJob(file)
            } else {
                if (logger.isTraceEnabled) {
                    Log.log(logger::warn, "fileOpened: file is relevant but document is null for {}", file)
                }
            }
        }
    }

    //this method is called on EDT and must be fast
    fun fileClosed(file: VirtualFile) {
        try {
            doOnFileClosed(file)
        }catch (e:Throwable){
            Log.warnWithException(logger,e,"Exception in fileClosed {}",e)
            ErrorReporter.getInstance().reportError(project, "EditorDocumentService.fileClosed", e)
        }
    }

    private fun doOnFileClosed(file: VirtualFile) {

        //usually we are not interested in non-writable files, they are library sources or vcs files etc.
        //although a user may mark a content file as non-writable, but this is probably very rare.
        if (!file.isWritable) {
            return
        }

        //Do the same check as in fileOpened, if we didn't process it in fileOpened then no need to do anything in fileClosed
        if (!isRelevantLanguageFileNoSlowOperations(file)) {
            return
        }

        if (logger.isTraceEnabled) {
            Log.log(logger::trace, "fileClosed: processing {}", file)
        }

        val disposable = documentChangeListenerDisposables.remove(file)
        disposable?.let {
            if (logger.isTraceEnabled) {
                Log.log(logger::trace, "fileClosed: removing document listener for {}", file)
            }
            Disposer.dispose(it)
        }

        cs.launch {
            try {
                //if the job is still running, cancel it, so it will not build document info for a closed file
                removeDocumentFromChangedDocuments(file)
                filesToUpdate.remove(file)
                runningJobs[file]?.cancel(CancellationException("File was closed"))
            } catch (e: CancellationException) {
                throw e // ⚠️ Always rethrow to propagate cancellation properly
            } catch (e: Throwable) {
                Log.warnWithException(logger, e, "Exception in fileClosed {}", e)
                ErrorReporter.getInstance().reportError(project, "EditorDocumentService.fileClosed", e)
            } finally {
                putRemoveLock.withLock {
                    DocumentInfoStorage.getInstance(project).removeDocumentInfo(file)
                }
            }
        }
    }


    private fun removeDocumentFromChangedDocuments(file: VirtualFile) {
        //We need to remove the corresponding document from the changedDocuments queue if it's there.
        //If it's there, and it's not removed, it will arrive to filesToUpdate in the next deduplication and we'll try
        // to build a DocumentInfo for it which will be redundant. Also, keeping it in the queue is a kind of memory
        // leak because when the file is closed, intellij will dispose of the Document object.

        //This is a costly operation and should be called only on file close.
        //We have to pay this price to use a queue for documents in documentChanged events instead
        // of converting the documents to virtual files on EDT.

        changedDocuments.removeIf { document ->
            FileDocumentManager.getInstance().getFile(document) == file
        }
    }


    private fun processDocumentQueue() {

        //Multiple change events can arrive for the same document.
        //This method deduplicates them by collecting virtual files into a set.
        //After deduplication check if there was a quiet period, if yes, update all files.
        var document = changedDocuments.poll()
        while (document != null) {
            if (document.isWritable) {
                val file = FileDocumentManager.getInstance().getFile(document)
                file?.let {
                    filesToUpdate.add(file)
                }
            }
            document = changedDocuments.poll()
        }

        filesToUpdate.takeIf { it.isNotEmpty() }?.let { files ->
            //this code waits for quite period before we update the document info.
            val cutoff = Instant.now().minus(quitePeriodForUpdateSeconds)
            if (lastUpdateTimestamps?.isBefore(cutoff) == true) {
                lastUpdateTimestamps = null
                files.forEach { file -> updateDocumentInfo(file) }
                files.clear()
            }
        }
    }

    //This method is invoked after the quite period has passed.
    //While waiting for quite period we didn't run anything on EDT or read access.
    private fun updateDocumentInfo(virtualFile: VirtualFile) {
        if (logger.isTraceEnabled) {
            Log.log(logger::trace, "updateDocumentInfo: {}", virtualFile)
        }

        launchBuildDocumentInfoJob(virtualFile)
    }


    //this method will be called on EDT from fileOpened, and in a coroutine for updates, it should run fast and not take read access.
    private fun launchBuildDocumentInfoJob(file: VirtualFile) {

        if (logger.isTraceEnabled) {
            Log.log(logger::trace, "launchBuildDocumentInfoJob: {}", file)
        }

        runningJobs[file]?.cancel(CancellationException("New job started"))
        val job = cs.launch {
            buildDocumentInfo(file)
        }

        runningJobs[file] = job
        job.invokeOnCompletion { cause ->
            //if the cause is not null and not CancellationException, then it means the job failed,
            if (cause != null && cause !is CancellationException) {
                Log.warnWithException(logger, project, cause, "launchBuildDocumentInfoJob.launch: job failed {}", cause)
                ErrorReporter.getInstance().reportError(project, "EditorDocumentService.launchBuildDocumentInfoJob.launch", cause)
            }
            runningJobs.remove(file)
        }
    }


    private suspend fun buildDocumentInfo(file: VirtualFile) {

        //The job may be canceled before document info is ready, for example, if the file was closed before
        // this coroutine is finished. So check if coroutine is active often.
        //Also, make sure not to save document info for a closed file. The file may close just before
        // building DocumentInfo finished and just before putting it in the storage, that is handled by putRemoveLock.
        //Exceptions are caught in the coroutine exception handler and reported in invokeOnCompletion.

        Log.log(logger::trace, "buildDocumentInfo: {}", file)

        val isRelevant = readAction {
            isRelevantFile(file)
        }
        coroutineContext.ensureActive()
        if (!isRelevant) {
            Log.log(logger::trace, "buildDocumentInfo: file is not relevant {}", file)
            return
        }

        if (!isSupportedLanguageFile(project, file)) {
            Log.log(logger::trace, "buildDocumentInfo: file is not a supported language {}", file)
            return
        }
        coroutineContext.ensureActive()

        val languageService = LanguageServiceProvider.getInstance(project).getLanguageService(file)
        if (languageService == null) {
            Log.log(logger::warn, "buildDocumentInfo: could not find language service for {}", file)
            return
        }

        if (!languageService.isSupportedFile(file)) {
            Log.log(logger::trace, "buildDocumentInfo: file is not supported by language service {}", file)
            return
        }

        coroutineContext.ensureActive()
        val documentInfo = languageService.buildDocumentInfo(file)
        if (documentInfo == null) {
            Log.log(logger::warn, "buildDocumentInfo: could not build document info for {}", file)
        } else {
            Log.log(logger::trace, "buildDocumentInfo: done building document info for {}", file)
            coroutineContext.ensureActive()
            putRemoveLock.withLock {
                coroutineContext.ensureActive()
                DocumentInfoStorage.getInstance(project).putDocumentInfo(file, documentInfo)
            }
        }
    }

    //fast check for EDT.
    //doesn't check is ProjectFileIndex.getInstance(project).isInContent(file) because it is considered a slow operation and intellij will warn about it.
    private fun isRelevantLanguageFileNoSlowOperations(file: VirtualFile): Boolean {
        return file.isValid &&
                file.isWritable &&
                !FileUtils.isLightVirtualFileBase(file) &&
                isSupportedLanguageFile(project, file)
    }


    //must run in read access. not on EDT because some checks are considered slow operations that are prohibited on EDT.
    private fun isRelevantFile(file: VirtualFile): Boolean {
        return !(!file.isWritable ||
                !ProjectFileIndex.getInstance(project).isInContent(file) ||
                !isValidVirtualFile(file) ||
                FileUtils.isLightVirtualFileBase(file))
    }

    override fun dispose() {
        cancelAllRunningJobs()
        changedDocuments.clear()
        filesToUpdate.clear()
    }

    private fun cancelAllRunningJobs() {
        for ((_, job) in runningJobs) {
            job.cancel(CancellationException("Cancelled by dispose"))
        }
        runningJobs.clear()
    }

}