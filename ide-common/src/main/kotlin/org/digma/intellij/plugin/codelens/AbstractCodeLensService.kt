package org.digma.intellij.plugin.codelens

import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPointerManager
import com.intellij.util.messages.MessageBusConnection
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.document.CodeLensProvider
import org.digma.intellij.plugin.document.DocumentInfoChanged
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.navigation.HomeSwitcherService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.psi.PsiUtils
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import org.digma.intellij.plugin.ui.ToolWindowShower
import java.awt.event.MouseEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors


abstract class AbstractCodeLensService(private val project: Project): Disposable {

    private val logger: Logger = Logger.getInstance(AbstractCodeLensService::class.java)

    private val codeLensProvider: CodeLensProvider = project.getService(CodeLensProvider::class.java)

    private val codeLensCache: MutableMap<String, CodeLensContainer> = ConcurrentHashMap()

    private val documentInfoChangedConnection: MessageBusConnection = project.messageBus.connect()

    private val codeLensProviderFactory: CodeLensProviderFactory = project.getService(CodeLensProviderFactory::class.java)


    private class CodeLensContainer{
        val codeLensList :MutableList<Pair<TextRange, CodeVisionEntry>> = mutableListOf()
    }


    override fun dispose() {
        documentInfoChangedConnection.dispose()
    }

    init {

        DumbService.getInstance(project).runWhenSmart {
            Log.log(logger::debug,"runWhenSmart invoked, restarting DaemonCodeAnalyzer")
            restartAll()
        }

        documentInfoChangedConnection.subscribe(DocumentInfoChanged.DOCUMENT_INFO_CHANGED_TOPIC,DocumentInfoChanged { psiFile: PsiFile ->
            val psiUri = PsiUtils.psiFileToUri(psiFile)
            Log.log(logger::trace, "got documentInfoChanged, restarting DaemonCodeAnalyzer for {}", psiUri)
            codeLensCache.remove(PsiUtils.psiFileToUri(psiFile))
            //restartFile(psiFile)
            restartAll()
        })
    }




    fun getCodeLens(psiFile: PsiFile): List<Pair<TextRange, CodeVisionEntry>> {

        Log.log(logger::trace, "got request for code lens for {}", psiFile.virtualFile)

        val psiUri = PsiUtils.psiFileToUri(psiFile)

        var codeLensContainer = codeLensCache[psiUri]
        //testing also isNotEmpty to prevent cache misses,the cache may be empty but maybe the document info
        // has changed and the cache was not notified, if It's still empty buildCodeLens will be fast anyway.
        if (codeLensContainer != null && codeLensContainer.codeLensList.isNotEmpty()){
            Log.log(logger::trace, "returning code lens from cache for {}", psiFile.virtualFile)
            return codeLensContainer.codeLensList
        }

        Log.log(logger::trace, "building code lens for {}", psiFile.virtualFile)
        codeLensContainer = buildCodeLens(psiFile)
        Log.log(logger::trace, "adding code lens to cache for {}", psiFile.virtualFile)
        codeLensCache[psiUri] = codeLensContainer
        return codeLensContainer.codeLensList
    }


    private fun buildCodeLens(psiFile: PsiFile):CodeLensContainer {

        val codeLensContainer = CodeLensContainer()

        val codeLenses = codeLensProvider.provideCodeLens(psiFile)
        val methods: Map<String, Pair<TextRange, PsiElement>> =
            findMethodsByCodeObjectIds(psiFile, codeLenses.stream().map { it.codeObjectId }.collect(Collectors.toSet()))

        val usedGenericProviders: MutableList<String> = ArrayList<String>()
        codeLenses.forEach { lens ->

            val methodPair = methods[lens.codeObjectId]
            methodPair?.let {
                val method = methodPair.second
                val textRange = methodPair.first

                val entry = ClickableTextCodeVisionEntry(
                    lens.lensTitle,
                    codeLensProviderFactory.getProviderId(lens.lensTitle, usedGenericProviders),
                    ClickHandler(method, lens.lensTitle, project),
                    null, // icon was set already on previous step inside CodeLensProvider.buildCodeLens()
                    lens.lensMoreText,
                    lens.lensDescription
                )
                codeLensContainer.codeLensList.add(Pair(textRange, entry))
            }
        }

        return codeLensContainer
    }

    abstract fun findMethodsByCodeObjectIds(psiFile: PsiFile, ids: Set<String>): Map<String, Pair<TextRange, PsiElement>>



    @Suppress("UNUSED_PARAMETER")
    fun environmentChanged(newEnv: String) {
        Log.log(logger::debug,"got environmentChanged, restarting DaemonCodeAnalyzer")
        restartAll()
    }



    private fun restartAll() {

        //todo: since CodeVisionPassFactory became internal its not possible
        // to call CodeVisionPassFactory.clearModificationStamp(it.editor) and just calling DaemonCodeAnalyzer.restart
        // does not trigger a call to computeForEditor and there is no refresh.
        // doing it the way bellow works, but seems a waste to clear and refresh all when we need to refresh only one file.
        // try to find a replacement for CodeVisionPassFactory.clearModificationStamp and refresh only one by one.

        if(project.isDisposed){
            return
        }

        codeLensCache.clear()

        EDT.ensureEDT {
            WriteAction.run<RuntimeException> {
                val manager = PsiManager.getInstance(project)
                manager.dropPsiCaches()
                manager.dropResolveCaches()
                DaemonCodeAnalyzer.getInstance(project).restart()
            }
        }

//        ReadAction.nonBlocking(RunnableCallable{
//            DaemonCodeAnalyzer.getInstance(project).restart()
//            Log.log(logger::debug,"restarting DaemonCodeAnalyzer for all files ")
//            codeLensCache.clear()
//            FileEditorManager.getInstance(project).openFiles.forEach {  virtualFile ->
//                val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
//                psiFile?.let {
//                    restartFile(it)
//                }
//            }
//        }).inSmartMode(project).submit(NonUrgentExecutor.getInstance())
    }

//    private fun restartFile(psiFile: PsiFile) {
//        FileEditorManager.getInstance(project).allEditors.forEach {
//            if (it.file == psiFile.virtualFile && it is TextEditor) {
//                Log.log(logger::debug, "restarting DaemonCodeAnalyzer for {}", psiFile.virtualFile)
//                DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
//            }
//        }
//    }


    private class ClickHandler(
        element: PsiElement,
        private val lensTitle: String,
        private val project: Project,
    ) : (MouseEvent?, Editor) -> Unit {
        private val elementPointer = SmartPointerManager.createPointer(element)
        override fun invoke(event: MouseEvent?, editor: Editor) {
            project.service<MainToolWindowCardsController>().closeCoveringViewsIfNecessary()
            ActivityMonitor.getInstance(project).registerLensClicked(lensTitle)
            project.service<HomeSwitcherService>().switchToInsights()
            ToolWindowShower.getInstance(project).showToolWindow()
            elementPointer.element?.let {
                if (it is Navigatable && it.canNavigateToSource()) {
                    it.navigate(true)
                } else {
                    //it's a fallback. sometimes the psiMethod.canNavigateToSource is false and really the
                    //navigation doesn't work. I can't say why. usually it happens when indexing is not ready yet,
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