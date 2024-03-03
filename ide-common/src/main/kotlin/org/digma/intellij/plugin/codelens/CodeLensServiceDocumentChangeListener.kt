package org.digma.intellij.plugin.codelens

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.util.Alarm
import com.intellij.util.AlarmFactory
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.common.isValidVirtualFile
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.psi.LanguageService
import org.digma.intellij.plugin.psi.PsiUtils

//any change in the current editor needs a code lens refresh.
//there is a listener for CodeLensChanged events, but its not enough. the file in the editor may change
// but code lens list will not change. in that case the lenses may lose position in the file.
//so, any change in the current editor should trigger a code lens refresh for the file.
// this listener will do that with 5 seconds of quite period.
class CodeLensServiceDocumentChangeListener(private val project: Project) : FileEditorManagerListener {

    private val logger: Logger = Logger.getInstance(this::class.java)

    private val disposables = mutableMapOf<VirtualFile, Disposable>()

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {

        try {
            if (!isProjectValid(project) || !isValidVirtualFile(file)) {
                return
            }

            //that means a listener is already installed
            if (disposables.containsKey(file)) {
                return
            }


            val languageService = LanguageService.findLanguageServiceByFile(project, file)

            //only some languages need code vision support
            if (!languageService.isCodeVisionSupported) {
                return
            }

            installDocumentListener(file, languageService)

        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "Exception in fileOpened")
            ErrorReporter.getInstance().reportError(project, "${this::class.simpleName}.fileOpened", e)
        }
    }


    private fun installDocumentListener(file: VirtualFile, languageService: LanguageService) {

        val psiFile = PsiManager.getInstance(project).findFile(file)
        if (PsiUtils.isValidPsiFile(psiFile) && languageService.isRelevant(file)) {
            val document = PsiDocumentManager.getInstance(project).getDocument(psiFile!!)
            if (document != null) {

                val parentDisposable = Disposer.newDisposable()
                disposables[file] = parentDisposable
                Log.log(logger::trace, "installing document listener for file {}", file)

                document.addDocumentListener(object : DocumentListener {

                    private val documentChangeAlarm = AlarmFactory.getInstance().create(
                        Alarm.ThreadToUse.POOLED_THREAD, parentDisposable
                    )

                    override fun documentChanged(event: DocumentEvent) {
                        try {
                            if (!isProjectValid(project)) {
                                return
                            }

                            documentChangeAlarm.cancelAllRequests()
                            documentChangeAlarm.addRequest({
                                try {
                                    val changedPsiFile = PsiDocumentManager.getInstance(project).getPsiFile(event.document)
                                    changedPsiFile?.let {
                                        if (PsiUtils.isValidPsiFile(it)) {
                                            project.service<CodeLensService>().refresh(it)
                                        }
                                    }
                                } catch (e: Throwable) {
                                    Log.warnWithException(logger, e, "Exception in documentChanged")
                                    ErrorReporter.getInstance().reportError(
                                        project,
                                        "${this::class.simpleName}.DocumentListener.documentChanged", e
                                    )
                                }
                            }, 5000)

                        } catch (e: Throwable) {
                            Log.warnWithException(logger, e, "Exception in documentChanged")
                            ErrorReporter.getInstance().reportError(project, "${this::class.simpleName}.DocumentListener.documentChanged", e)
                        }
                    }
                }, parentDisposable)
            }
        }
    }


    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        try {
            val disposable = disposables.remove(file)
            if (disposable != null) {
                Log.log(logger::trace, "disposing disposable for file:{}", file)
                Disposer.dispose(disposable)
            }
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "${this::class.simpleName}.fileClosed", e)
        }
    }
}