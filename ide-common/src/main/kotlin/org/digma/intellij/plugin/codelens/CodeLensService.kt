package org.digma.intellij.plugin.codelens

import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.codeInsight.hints.InlayHintsUtils
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.jetbrains.rd.util.ConcurrentHashMap
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.document.CodeLensChanged
import org.digma.intellij.plugin.document.CodeLensProvider
import org.digma.intellij.plugin.document.CodeLensUtils.psiFileToKey
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.lens.CodeLens
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.psi.LanguageService
import org.digma.intellij.plugin.scope.ScopeManager
import org.digma.intellij.plugin.scope.SpanScope
import java.awt.event.MouseEvent

//don't convert to light service because it will register on all IDEs, but we want it only on Idea and Pycharm
@Suppress("LightServiceMigrationCode")
class CodeLensService(private val project: Project) : Disposable {

    private val logger: Logger = Logger.getInstance(CodeLensService::class.java)

    //cache psiFile -> map providerId -> CodeLensContainer
    private val cachedCodeVision = ConcurrentHashMap(mutableMapOf<String, MutableMap<String, CodeLensContainer>>())

    private val codeVisionRefresh = CodeVisionRefresh(project)


    init {
        project.messageBus.connect(this).subscribe(CodeLensChanged.CODELENS_CHANGED_TOPIC, object : CodeLensChanged {
            override fun codelensChanged(psiFile: PsiFile) {
                try {
                    Log.log(logger::trace, "codelensChanged called for file {}", psiFile)
                    refreshOneFile(psiFile)
                } catch (e: Throwable) {
                    ErrorReporter.getInstance().reportError("CodeLensService.codelensChanged", e)
                }
            }

            override fun codelensChanged(psiFilesUrls: List<String>) {
                try {
                    Log.log(logger::trace, "codelensChanged called for files {}", psiFilesUrls)
                    refreshFiles(psiFilesUrls)
                } catch (e: Throwable) {
                    ErrorReporter.getInstance().reportError("CodeLensService.codelensChanged", e)
                }
            }

            override fun codelensChanged() {
                try {
                    Log.log(logger::trace, "codelensChanged called")
                    refreshAll()
                } catch (e: Throwable) {
                    ErrorReporter.getInstance().reportError("CodeLensService.codelensChanged", e)
                }
            }
        })

    }


    override fun dispose() {
        cachedCodeVision.clear()
    }

    fun getCodeLens(providerId: String, psiFile: PsiFile, languageService: LanguageService): List<Pair<TextRange, CodeVisionEntry>> {

        Log.log(logger::trace, "getCodeLens called for provider {} for file {}", providerId, psiFile)

        val cached = cachedCodeVision[psiFileToKey(psiFile)]?.get(providerId)
        if (cached != null) {
            Log.log(logger::trace, "returning cached code vision for provider {} for file {}", providerId, psiFile)
            return cached.codeLensList
        }


        val codeLensesForProvider = selectCodeLensesForProvider(providerId, psiFile)
        Log.log(logger::trace, "got codeLensesForProvider for provider {} for file {} [{}]", providerId, psiFile, codeLensesForProvider)

        val codeLensContainer = CodeLensContainer()

        codeLensesForProvider?.let { lenses ->

            val methodIds = lenses.map { it.codeMethod }
            val methodsPsiElements: Map<String, PsiElement> = languageService.findMethodsByCodeObjectIds(psiFile, methodIds)

            lenses.forEach { lens ->

                Log.log(logger::trace, "adding code vision for provider {} for file {} [{}]", providerId, psiFile, lens)

                val psiMethod = methodsPsiElements[lens.codeMethod]
                psiMethod?.let { method ->

                    val entry = ClickableTextCodeVisionEntry(
                        lens.lensTitle,
                        providerId,
                        ClickHandler(method, lens, project),
                        null, // icon was set already on previous step inside CodeLensProvider.buildCodeLens()
                        lens.lensMoreText,
                        lens.lensDescription
                    )

                    @Suppress("UnstableApiUsage")
                    val textRange = InlayHintsUtils.getTextRangeWithoutLeadingCommentsAndWhitespaces(method)
                    codeLensContainer.codeLensList.add(Pair(textRange, entry))
                }
            }
        }

        cachedCodeVision.computeIfAbsent(psiFileToKey(psiFile)) { mutableMapOf() }[providerId] = codeLensContainer

        return codeLensContainer.codeLensList
    }


    private fun selectCodeLensesForProvider(providerId: String, psiFile: PsiFile): Set<CodeLens>? {

        val codeLensesForFile = CodeLensProvider.getInstance(project).provideCodeLens(psiFile)

        val providerLensSelector = project.service<CodeVisionProviderToLensSelector>()

        //each DigmaCodeVisionProviderBase provides lens for the same lens id.
        //CodeVisionProviderToLensSelector will always return the same lens id for this provider.
        //it is a random per session/project selection, but makes sure this provider will always
        // provide for the same lens id for this project in this IDE session.
        //if this provider already assigned a lens id it is returned, otherwise the next unassigned
        // lens id will be selected.
        //if all providers are already assigned a lens id and there are more lens types then providers
        // then some lens types will not be shown.
        //there are 30 providers , that should be enough. if there are more than 30 lens types then
        // we need more providers.
        val providerLensId = providerLensSelector.selectLensForProvider(providerId, codeLensesForFile)

        return providerLensId?.let { lensId ->
            Log.log(logger::trace, "selected code lens {} for provider {}", lensId, providerId)
            codeLensesForFile.filter { it.id == lensId }.toSet()
        }

    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun clearCacheForFile(psiFile: PsiFile) {
        cachedCodeVision.remove(psiFileToKey(psiFile))
    }

    private fun clearCacheForFiles(psiFilesUrls: List<String>) {
        psiFilesUrls.forEach {
            cachedCodeVision.remove(it)
        }
    }


    fun clearCache() {
        cachedCodeVision.clear()
    }


    fun refreshOneFile(psiFile: PsiFile) {
        Log.log(logger::trace, "refresh called for file {}", psiFile)
        clearCacheForFile(psiFile)
        codeVisionRefresh.refreshForFile(psiFile)
    }


    fun refreshFiles(psiFilesUrls: List<String>) {
        Log.log(logger::trace, "refresh called for files {}", psiFilesUrls)
        //sending the psi urls to remove. urls came from CodeLensProvider and should be the same as used
        // here as keys, all these services use org.digma.intellij.plugin.document.CodeLensUtils.psiFileToKey.
        clearCacheForFiles(psiFilesUrls)
        codeVisionRefresh.refreshForFiles(psiFilesUrls)
    }


    fun refreshAll() {
        Log.log(logger::trace, "refresh called for all")
        clearCache()
        codeVisionRefresh.refreshAll()
    }



    private class CodeLensContainer {
        val codeLensList: MutableList<Pair<TextRange, CodeVisionEntry>> = mutableListOf()
    }


    private class ClickHandler(
        element: PsiElement,
        private val lens: CodeLens,
        private val project: Project,
    ) : (MouseEvent?, Editor) -> Unit {
        private val logger: Logger = Logger.getInstance(this::class.java)
        private val elementPointer = SmartPointerManager.createPointer(element)
        override fun invoke(event: MouseEvent?, editor: Editor) {
            try {
                ActivityMonitor.getInstance(project).registerLensClicked(lens.id)
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
                        selectedEditor?.caretModel?.moveToOffset(it.textOffset)
                    }
                }
                Backgroundable.ensurePooledThread {
                    if (lens.scopeCodeObjectId.startsWith("span:")) {
                        ScopeManager.getInstance(project).changeScope(SpanScope(lens.scopeCodeObjectId))
                    }
                }

            } catch (e: Exception) {
                Log.warnWithException(logger, project, e, "error in ClickHandler {}", e)
                ErrorReporter.getInstance().reportError(project, "${this::class.simpleName}.ClickHandler.invoke", e)
            }
        }
    }


}