package org.digma.intellij.plugin.idea.psi.java

import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.hints.InlayHintsUtils
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SyntaxTraverser
import com.intellij.util.messages.MessageBusConnection
import org.digma.intellij.plugin.document.CodeLensProvider
import org.digma.intellij.plugin.document.DocumentInfoChanged
import org.digma.intellij.plugin.model.InsightImportance
import org.digma.intellij.plugin.psi.PsiUtils
import org.digma.intellij.plugin.ui.ToolWindowShower
import java.awt.event.MouseEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors
import javax.swing.Icon

class JavaCodeLensService(private val project: Project): Disposable {

    private val codeLensProvider: CodeLensProvider = project.getService(CodeLensProvider::class.java)

    private val codeLensCache: MutableMap<String, CodeLensContainer> = ConcurrentHashMap()

    private val errorHotspotIcon = IconLoader.findIcon("icons/exclMark.svg", this.javaClass.classLoader)

    private val documentInfoChangedConnection: MessageBusConnection = project.messageBus.connect()

    companion object {
        @JvmStatic
        fun getInstance(project: Project): JavaCodeLensService {
            return project.getService(JavaCodeLensService::class.java)
        }
    }


    override fun dispose() {
        documentInfoChangedConnection.dispose()
    }

    init {
        documentInfoChangedConnection.subscribe(DocumentInfoChanged.DOCUMENT_INFO_CHANGED_TOPIC, DocumentInfoChanged {psiFile: PsiFile ->
            ApplicationManager.getApplication().runReadAction {
                codeLensCache.remove(PsiUtils.psiFileToUri(psiFile))
                DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
            }
        })
    }



    private class CodeLensContainer{
        val codeLensList :MutableList<Pair<TextRange, CodeVisionEntry>> = mutableListOf()
    }

    fun getCodeLens(psiFile: PsiFile): List<Pair<TextRange, CodeVisionEntry>> {
        if (!codeLensCache.containsKey(PsiUtils.psiFileToUri(psiFile))){
            buildCodeLens(psiFile)
        }

        return codeLensCache[PsiUtils.psiFileToUri(psiFile)]!!.codeLensList
    }


    private fun buildCodeLens(psiFile: PsiFile) {

        synchronized(psiFile){

            val codeLensContainer = codeLensCache.computeIfAbsent(PsiUtils.psiFileToUri(psiFile)) { CodeLensContainer() }
            codeLensContainer.codeLensList.clear()

            val codeLenses = codeLensProvider.provideCodeLensNoError(psiFile)
            val methods: Map<String, Pair<TextRange,PsiMethod>> =
                findMethodsByCodeObjectIds(psiFile, codeLenses.stream().map { it.codeObjectId }.collect(Collectors.toSet()))

            codeLenses.forEach { lens ->

                val methodPair = methods[lens.codeObjectId]
                methodPair?.let {
                    val method = methodPair.second
                    val textRange = methodPair.first

                    val entry = ClickableTextCodeVisionEntry(
                            lens.lensTitle,
                            JavaCodeVisionProvider.JavaCodeLens.ID,
                            ClickHandler(method, project),
                            getIconByImportanceLevel(lens.importance),
                            lens.lensMoreText,
                            lens.lensDescription
                    )
                    codeLensContainer.codeLensList.add(Pair(textRange, entry))
                }
            }

        }
    }


    private fun findMethodsByCodeObjectIds(psiFile: PsiFile, ids: Set<String>): Map<String, Pair<TextRange,PsiMethod>> {

        return ReadAction.compute<Map<String, Pair<TextRange,PsiMethod>>,Exception> {

            val methods = mutableMapOf<String, Pair<TextRange,PsiMethod>>()
            val traverser = SyntaxTraverser.psiTraverser(psiFile)
            for (element in traverser) {
                if (element is PsiMethod) {
                    val codeObjectId = JavaLanguageUtils.createJavaMethodCodeObjectId(element)
                    if (ids.contains(codeObjectId)) {
                        val textRange = InlayHintsUtils.getTextRangeWithoutLeadingCommentsAndWhitespaces(element)
                        methods[codeObjectId] = Pair(textRange,element)
                    }
                }
            }

            return@compute methods
        }
    }



    fun environmentChanged(newEnv: String) {
        codeLensCache.clear()
        ApplicationManager.getApplication().runReadAction {
            val fileEditor = FileEditorManager.getInstance(project).selectedEditor
            if (fileEditor != null) {
                val file = fileEditor.file
                val psiFile = PsiManager.getInstance(project).findFile(file)
                if (psiFile != null) {
                    DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
                }
            }
        }
    }

    private fun getIconByImportanceLevel(importanceLevel: Int): Icon? {
        return if (isImportant(importanceLevel)) {
            errorHotspotIcon
        } else {
            null
        }
    }
    private fun isImportant(importance: Int): Boolean {
        return importance <= InsightImportance.HighlyImportant.priority &&
                importance >= InsightImportance.ShowStopper.priority
    }


    private class ClickHandler(
        element: PsiMethod,
        private val project: Project
    ) : (MouseEvent?, Editor) -> Unit {
        private val elementPointer = SmartPointerManager.createPointer(element)
        private val toolWindowShower = project.getService(ToolWindowShower::class.java)
        override fun invoke(event: MouseEvent?, editor: Editor) {
            toolWindowShower.showToolWindow()
            elementPointer.element?.let {
                if (elementPointer.element?.canNavigateToSource() == true) {
                    elementPointer.element?.navigate(true)
                } else {
                    //it's a fallback. sometimes the psiMethod.canNavigateToSource is false and really the
                    //navigation doesn't work. i can't say why. usually it happens when indexing is not ready yet,
                    // and the user opens files, selects tabs or moves the caret. then when indexing is finished
                    // we have the list of methods but then psiMethod.navigate doesn't work.
                    // navigation to source using the editor does work in these circumstances.
                    val selectedEditor = FileEditorManager.getInstance(project).selectedTextEditor
                    selectedEditor?.caretModel?.moveToOffset(elementPointer.element!!.textOffset)
                }
            }

        }
    }


}