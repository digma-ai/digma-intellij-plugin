package org.digma.intellij.plugin.codelens

import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.hints.codeVision.CodeVisionPassFactory
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.pom.Navigatable
import com.intellij.psi.*
import com.intellij.util.RunnableCallable
import com.intellij.util.concurrency.NonUrgentExecutor
import com.intellij.util.messages.MessageBusConnection
import org.apache.commons.collections4.map.LRUMap
import org.digma.intellij.plugin.document.CodeLensProvider
import org.digma.intellij.plugin.document.DocumentInfoChanged
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.psi.PsiUtils
import org.digma.intellij.plugin.ui.ToolWindowShower
import java.awt.event.MouseEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import java.util.stream.Collectors
import kotlin.concurrent.withLock


abstract class AbstractCodeLensService(private val project: Project): Disposable {

    private val logger: Logger = Logger.getInstance(AbstractCodeLensService::class.java)

    private val codeLensProvider: CodeLensProvider = project.getService(CodeLensProvider::class.java)

    private val codeLensCache: MutableMap<String, CodeLensContainer> = ConcurrentHashMap()

    private val documentInfoChangedConnection: MessageBusConnection = project.messageBus.connect()

    private val codeLensProviderFactory: CodeLensProviderFactory = project.getService(CodeLensProviderFactory::class.java)

    private val psiLocks = Collections.synchronizedMap(LRUMap<String, ReentrantLock>(30))

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
            val lock = psiLocks.computeIfAbsent(psiUri) { ReentrantLock() }
            Log.log(logger::debug, "got documentInfoChanged, restarting DaemonCodeAnalyzer for {}", psiUri)
            lock.withLock {
                codeLensCache.remove(PsiUtils.psiFileToUri(psiFile))
            }
            ReadAction.nonBlocking(RunnableCallable{
                restartFile(psiFile)
            }).inSmartMode(project).submit(NonUrgentExecutor.getInstance())
        })
    }



    private class CodeLensContainer{
        val codeLensList :MutableList<Pair<TextRange, CodeVisionEntry>> = mutableListOf()
    }

    fun getCodeLens(psiFile: PsiFile): List<Pair<TextRange, CodeVisionEntry>> {

        Log.log(logger::debug,"got request for code lens for {}",psiFile.virtualFile)

        val psiUri = PsiUtils.psiFileToUri(psiFile)

        //first try to get codeLensContainer without locking, if exists , return it,
        // if some thread calls remove after that, we don't care, it will be re-populated.
        val codeLensContainer = codeLensCache[psiUri]
        if (codeLensContainer != null){
            Log.log(logger::debug,"returning code lens from cache for {}",psiFile.virtualFile)
            return codeLensContainer.codeLensList
        }

        val lock = psiLocks.computeIfAbsent(psiUri) { ReentrantLock() }
        lock.withLock {
            if (!codeLensCache.containsKey(psiUri)) {
                Log.log(logger::debug,"building code lens for {}",psiFile.virtualFile)
                @Suppress("NAME_SHADOWING")
                val codeLensContainer = buildCodeLens(psiFile)
                codeLensCache[psiUri] = codeLensContainer
            }

            return codeLensCache[psiUri]!!.codeLensList
        }
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
                    ClickHandler(method, project),
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
        ReadAction.nonBlocking(RunnableCallable{
            Log.log(logger::debug,"restarting DaemonCodeAnalyzer for all files ")
            codeLensCache.clear()
            FileEditorManager.getInstance(project).openFiles.forEach {
                val psiFile = PsiManager.getInstance(project).findFile(it)
                psiFile?.let { psiFile ->
                    restartFile(psiFile)
                }
            }
        }).inSmartMode(project).submit(NonUrgentExecutor.getInstance())
    }

    private fun restartFile(psiFile: PsiFile) {
        FileEditorManager.getInstance(project).allEditors.forEach {
            if (it.file == psiFile.virtualFile && it is TextEditor) {
                Log.log(logger::debug, "restarting DaemonCodeAnalyzer for {}", psiFile.virtualFile)
                CodeVisionPassFactory.clearModificationStamp(it.editor)
                DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
            }
        }
    }


    private class ClickHandler(
        element: PsiElement,
        private val project: Project
    ) : (MouseEvent?, Editor) -> Unit {
        private val elementPointer = SmartPointerManager.createPointer(element)
        override fun invoke(event: MouseEvent?, editor: Editor) {
            ToolWindowShower.getInstance(project).showToolWindow()
            elementPointer.element?.let {
                if (it is Navigatable && it.canNavigateToSource()) {
                    it.navigate(true)
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