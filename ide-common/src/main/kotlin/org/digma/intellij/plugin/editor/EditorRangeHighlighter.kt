package org.digma.intellij.plugin.editor

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
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
import org.digma.intellij.plugin.psi.LanguageServiceLocator
import java.util.concurrent.locks.ReentrantLock

@Service(Service.Level.PROJECT)
class EditorRangeHighlighter(private val project: Project) : Disposable {

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
                //locking to make sure we don't miss a clearHighlighter.
                // in case clearHighlighter is called before this method completes we may miss the clear
                // and the highlighter will stay on
                myLock.lock()

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

                        highlighters[methodId] = Pair(editor, rangeHighlighter)
                    }
                }

            } finally {
                if (myLock.isHeldByCurrentThread) {
                    myLock.unlock()
                }
            }

        }
    }


    fun clearHighlighter(methodId: String) {

        try {

            myLock.lock()

            EDT.ensureEDT {
                highlighters[methodId]?.let {
                    removeHighlighter(it.first, it.second)
                }
            }
        } finally {
            if (myLock.isHeldByCurrentThread) {
                myLock.unlock()
            }
        }
    }


    fun clearAllHighlighters() {

        try {

            myLock.lock()

            EDT.ensureEDT {
                highlighters.forEach { entry ->
                    entry.value.let {
                        removeHighlighter(it.first, it.second)
                    }
                }
            }
        } finally {
            if (myLock.isHeldByCurrentThread) {
                myLock.unlock()
            }
        }
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