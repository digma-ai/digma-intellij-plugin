package org.digma.intellij.plugin.editor

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.psi.LanguageServiceLocator
import java.util.concurrent.locks.ReentrantLock

@Service(Service.Level.PROJECT)
class EditorRangeHighlighter(private val project: Project) : Disposable {

    private val logger = Logger.getInstance(this::class.java)

    private val highlighters = mutableMapOf<String, Pair<Editor, RangeHighlighter>>()

    private val myLock = ReentrantLock()

    companion object {
        @JvmStatic
        fun getInstance(project: Project): EditorRangeHighlighter {
            return project.service<EditorRangeHighlighter>()
        }
    }


    override fun dispose() {
        clearAllHighlighters()
    }


    fun highlightMethod(methodId: String) {

        EDT.ensureEDT {
            try {

                Log.log(logger::trace, "adding highlighter for {}", methodId)

                //locking to make sure we don't miss a clearHighlighter.
                // in case clearHighlighter is called before this method completes we may miss the clear
                // and the highlighter will stay on
                myLock.lock()

                //first clear in case this method already has a highlighter
                clearHighlighter(methodId)

                val selectedTextEditor = FileEditorManager.getInstance(project).selectedTextEditor

                selectedTextEditor?.let { editor ->

                    val psiMethod = findPsiMethod(methodId, editor.document)

                    psiMethod?.also { method ->
                        val rangeHighlighter = editor.markupModel.addRangeHighlighter(
                            EditorColors.LIVE_TEMPLATE_ATTRIBUTES,
                            method.startOffset,
                            method.endOffset,
                            9999,
                            HighlighterTargetArea.EXACT_RANGE
                        )

                        Log.log(logger::trace, "highlighter for {} added", methodId)
                        highlighters[methodId] = Pair(editor, rangeHighlighter)
                    }
                }

            } catch (e: Throwable) {
                Log.warnWithException(logger, project, e, "error adding highlighter {}", e)
                ErrorReporter.getInstance().reportError("", e)
            } finally {
                if (myLock.isHeldByCurrentThread) {
                    myLock.unlock()
                }
            }

        }
    }


    fun clearHighlighter(methodId: String) {

        EDT.ensureEDT {
            try {
                myLock.lock()
                highlighters.remove(methodId)?.let {
                    Log.log(logger::trace, "removing highlighter for {}", methodId)
                    removeHighlighter(it.first, it.second)
                }
            } catch (e: Throwable) {
                Log.warnWithException(logger, project, e, "error removing highlighter {}", e)
                ErrorReporter.getInstance().reportError("", e)
            } finally {
                if (myLock.isHeldByCurrentThread) {
                    myLock.unlock()
                }
            }
        }
    }


    fun clearAllHighlighters() {
        Log.log(logger::trace, "removing all highlighters")
        //create a new list to avoid concurrent modification
        highlighters.keys.toList().forEach { clearHighlighter(it) }
    }


    private fun removeHighlighter(editor: Editor, rangeHighlighter: RangeHighlighter) {
        editor.markupModel.removeHighlighter(rangeHighlighter)
    }


    private fun findPsiMethod(methodId: String, document: Document): PsiElement? {

        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)

        return psiFile?.let {
            val languageService = LanguageServiceLocator.getInstance(project).locate(it.language)
            languageService.getPsiElementForMethod(methodId)
        }
    }


}