package org.digma.intellij.plugin.editor

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.intellij.util.concurrency.annotations.RequiresEdt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.psi.findMethodPsiElementByMethodId
import java.util.concurrent.locks.ReentrantLock

@Service(Service.Level.PROJECT)
class EditorRangeHighlighter(private val project: Project) : Disposable {

    private val logger = Logger.getInstance(this::class.java)

    private val highlighters = mutableMapOf<String, Pair<Editor, RangeHighlighter>>()

    private val myLock = ReentrantLock(true)

    companion object {
        @JvmStatic
        fun getInstance(project: Project): EditorRangeHighlighter {
            return project.service<EditorRangeHighlighter>()
        }
    }


    override fun dispose() {
        clearAllHighlighters()
    }


    suspend fun highlightMethod(methodId: String) {

        val psiMethod = findMethodPsiElementByMethodId(project, methodId)
        if (psiMethod == null) {
            Log.log(logger::trace, "methodId {} not found, no highlighter added", methodId)
            return
        }

        withContext(Dispatchers.EDT) {
            try {
                Log.trace(logger, "adding highlighter for {}", methodId)

                //locking to make sure we don't miss a clearHighlighter.
                // in case clearHighlighter is called before this method completes we may miss the clear
                // and the highlighter will stay on
                myLock.lock()
                //first clear in case this method already has a highlighter
                clearHighlighter(methodId)
                val selectedTextEditor = FileEditorManager.getInstance(project).selectedTextEditor
                selectedTextEditor?.let { editor ->
                    val rangeHighlighter = editor.markupModel.addRangeHighlighter(
                        EditorColors.LIVE_TEMPLATE_ATTRIBUTES,
                        psiMethod.startOffset,
                        psiMethod.endOffset,
                        9999,
                        HighlighterTargetArea.EXACT_RANGE
                    )

                    Log.trace(logger, "highlighter for {} added", methodId)
                    highlighters[methodId] = Pair(editor, rangeHighlighter)
                }

            } catch (e: Throwable) {
                Log.warnWithException(logger, project, e, "error adding highlighter {}", e)
                ErrorReporter.getInstance().reportError(project, "EditorRangeHighlighter.highlightMethod", e)
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
                    Log.trace(logger, "removing highlighter for {}", methodId)
                    removeHighlighter(it.first, it.second)
                }
            } catch (e: Throwable) {
                Log.warnWithException(logger, project, e, "error removing highlighter {}", e)
                ErrorReporter.getInstance().reportError(project, "EditorRangeHighlighter.clearHighlighter", e)
            } finally {
                if (myLock.isHeldByCurrentThread) {
                    myLock.unlock()
                }
            }
        }
    }


    fun clearAllHighlighters() {
        Log.trace(logger, "removing all highlighters")
        //create a new list to avoid concurrent modification
        highlighters.keys.toList().forEach { clearHighlighter(it) }
    }


    @RequiresEdt
    private fun removeHighlighter(editor: Editor, rangeHighlighter: RangeHighlighter) {
        editor.markupModel.removeHighlighter(rangeHighlighter)
    }
}