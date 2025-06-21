package org.digma.intellij.plugin.discovery

import com.intellij.openapi.application.readAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeAnyChangeAbstractAdapter
import com.intellij.psi.util.PsiModificationTracker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import org.digma.intellij.plugin.common.isValidVirtualFile
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.kotlin.ext.asyncWithErrorReporting
import org.digma.intellij.plugin.kotlin.ext.launchWithErrorReporting
import org.digma.intellij.plugin.log.Log
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class FileProcessingMonitor(
    private val project: Project,
    private val logger: Logger,
    private val checkInterval: Duration = 100.milliseconds
) {

    /**
     * Executes a block of code with file change monitoring.
     * Cancels the operation if file changes or PSI/stub mismatch is detected.
     */
    suspend fun <T> executeWithFileMonitoring(
        virtualFile: VirtualFile,
        operation: suspend () -> T
    ): ProcessingResult<T> = coroutineScope {

        val psiFile = readAction { PsiManager.getInstance(project).findFile(virtualFile) }
        if (psiFile == null) {
            return@coroutineScope ProcessingResult.Error("Psi file is null for file ${virtualFile.url}")
        }

        val psiTreeChangeDisposable = Disposer.newDisposable()

        //Start monitoring.
        //The monitorJob will be canceled when a change in the file is detected.
        //When the monitorJob is canceled, the select clause will select it and return ProcessingResult.Cancelled.
        //The operationJob will be canceled in the finally block, and so the file discovery will be canceled.

        val monitorJob = launchWithErrorReporting("FileProcessingMonitor.executeWithFileMonitoring", logger) {
            val initialSnapshot = createFileSnapshot(virtualFile, psiFile)
            if (initialSnapshot == FileProcessingSnapshot.INVALID) {
                Log.trace(logger, project, "FileProcessingMonitor: Cancelling file processing for {} because snapshot is invalid", virtualFile.url)
                throw CancellationException("File processing cancelled: initial snapshot invalid")
            }
            if (!initialSnapshot.isVirtualFileValid || !initialSnapshot.isPsiFileValid) {
                Log.trace(logger, project, "FileProcessingMonitor: Cancelling file processing for {} because virtual file or psi file is invalid", virtualFile.url)
                throw CancellationException("File processing cancelled: virtual file or psi file invalid")
            }

            val psiChanged = AtomicBoolean(false)
            PsiManager.getInstance(project).addPsiTreeChangeListener(object : PsiTreeAnyChangeAbstractAdapter() {
                override fun onChange(file: PsiFile?) {
                    file?.let {
                        if (it.virtualFile == virtualFile) {
                            psiChanged.set(true)
                        }
                    }
                }
            }, psiTreeChangeDisposable)


            while (isActive) {
                if (psiChanged.get()) {
                    throw CancellationException("File processing cancelled: psi tree changed")
                }
                delay(checkInterval)
                if (psiChanged.get()) {
                    throw CancellationException("File processing cancelled: psi tree changed")
                }
                monitorFileChanges(virtualFile, psiFile, initialSnapshot)
            }
        }

        // Execute the operation
        val operationJob = asyncWithErrorReporting("FileProcessingMonitor.executeWithFileMonitoring", logger) {
            operation()
        }

        try {
            //Select the first job that completes.
            //If monitorJob was canceled due to change in the file, it will be selected and the finally block will cancel
            // the operationJob.
            //If the operationJob completed, the processing result will be returned, and the finally block will cancel
            // the monitorJob
            //The select clause is biased towards the first job.
            select<ProcessingResult<T>> {
                operationJob.onAwait { ProcessingResult.Success(it) }
                monitorJob.onJoin { ProcessingResult.Cancelled("File monitoring detected changes") }
            }
        } catch (@Suppress("IncorrectCancellationExceptionHandling") e: CancellationException) {
            //this is a CancellationException from operationJob.onAwait. it should not be thrown. it will be thrown here in case the operation
            // was canceled somewhere else
            ProcessingResult.Cancelled(e.message ?: "File monitoring cancelled")
        } catch (e: Exception) {
            ProcessingResult.Error("Processing failed: ${e.message}", e)
        } finally {
            monitorJob.cancel()
            operationJob.cancel()
            Disposer.dispose(psiTreeChangeDisposable)
        }
    }

    /**
     * Monitor file for changes and PSI/stub consistency
     */
    private suspend fun monitorFileChanges(file: VirtualFile, psiFile: PsiFile, initialSnapshot: FileProcessingSnapshot) {
        val currentSnapshot = createFileSnapshot(file, psiFile)
        coroutineContext.ensureActive()
        val changeReason = detectChanges(initialSnapshot, currentSnapshot)
        coroutineContext.ensureActive()
        if (changeReason != null) {
            Log.trace(logger, project, "FileProcessingMonitor: Cancelling file processing for {}: {}", file.url, changeReason)
            throw CancellationException("File processing cancelled: $changeReason")
        }
    }

    /**
     * Create a snapshot of the file's current state
     */
    private suspend fun createFileSnapshot(file: VirtualFile, psiFile: PsiFile): FileProcessingSnapshot {
        return try {
            readAction {
                FileProcessingSnapshot(
                    virtualFileStamp = file.modificationStamp,
                    isVirtualFileValid = isValidVirtualFile(file),
                    psiModificationCount = PsiModificationTracker.getInstance(project).modificationCount,
                    psiFileStamp = psiFile.modificationStamp,
                    isPsiFileValid = psiFile.isValid,
                    fileLength = file.length,
                    psiTextLength = psiFile.textLength
                )

            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.warnWithException(logger, project, e, "FileProcessingMonitor: Failed to create snapshot for {}", file.url)
            ErrorReporter.getInstance().reportError(project, "FileProcessingMonitor.createFileSnapshot", e)
            FileProcessingSnapshot.INVALID
        }
    }


    /**
     * Detect what changed between snapshots
     */
    private fun detectChanges(
        initial: FileProcessingSnapshot,
        current: FileProcessingSnapshot
    ): String? {
        return when {
            !current.isVirtualFileValid -> "Virtual file became invalid"

            current.virtualFileStamp != initial.virtualFileStamp ->
                "Virtual file modified"

            !current.isPsiFileValid && initial.isPsiFileValid ->
                "PSI file became invalid"

            current.psiFileStamp != initial.psiFileStamp && current.psiFileStamp != -1L ->
                "PSI file modified"

            current.psiModificationCount != initial.psiModificationCount ->
                "PSI tree globally modified"

            current.fileLength != initial.fileLength ->
                "File length changed"

            current.psiTextLength != initial.psiTextLength ->
                "PSI text length changed"

            else -> null
        }
    }
}

/**
 * Snapshot of file state for change detection
 */
data class FileProcessingSnapshot(
    val virtualFileStamp: Long,
    val isVirtualFileValid: Boolean,
    val psiModificationCount: Long,
    val psiFileStamp: Long,
    val isPsiFileValid: Boolean,
    val fileLength: Long,
    val psiTextLength: Int,
) {
    companion object {
        val INVALID = FileProcessingSnapshot(
            virtualFileStamp = -1,
            isVirtualFileValid = false,
            psiModificationCount = -1,
            psiFileStamp = -1,
            isPsiFileValid = false,
            fileLength = -1,
            psiTextLength = -1,
        )
    }
}

/**
 * Result of a file processing operation
 */
sealed class ProcessingResult<T> {
    data class Success<T>(val result: T) : ProcessingResult<T>()
    data class Cancelled<T>(val reason: String) : ProcessingResult<T>()
    data class Error<T>(val message: String, val exception: Exception? = null) : ProcessingResult<T>()
}