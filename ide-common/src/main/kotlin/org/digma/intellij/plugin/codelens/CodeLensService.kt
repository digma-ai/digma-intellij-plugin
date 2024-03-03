package org.digma.intellij.plugin.codelens

import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.hints.InlayHintsUtils
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPointerManager
import com.jetbrains.rd.util.ConcurrentHashMap
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.document.CodeLensChanged
import org.digma.intellij.plugin.document.CodeLensProvider
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.lens.CodeLens
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.psi.LanguageService
import org.digma.intellij.plugin.psi.PsiUtils
import org.digma.intellij.plugin.scope.ScopeManager
import org.digma.intellij.plugin.scope.SpanScope
import java.awt.event.MouseEvent

//don't convert to light service because it will register on all IDEs, but we want it only on Idea and Pycharm
@Suppress("LightServiceMigrationCode")
class CodeLensService(private val project: Project) : Disposable {

    private val logger: Logger = Logger.getInstance(CodeLensService::class.java)

    //cache psiFile -> map providerId -> CodeLensContainer
    private val cachedCodeVision = ConcurrentHashMap(mutableMapOf<String, MutableMap<String, CodeLensContainer>>())


    init {
        project.messageBus.connect(this).subscribe(CodeLensChanged.CODELENS_CHANGED_TOPIC, object : CodeLensChanged {
            override fun codelensChanged(psiFile: PsiFile) {
                try {
                    Log.log(logger::trace, "codelensChanged called for file {}", psiFile)
                    refresh(psiFile)
                } catch (e: Throwable) {
                    ErrorReporter.getInstance().reportError("CodeLensService.codelensChanged", e)
                }
            }

            override fun codelensChanged() {
                try {
                    Log.log(logger::trace, "codelensChanged called")
                    refresh()
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

    fun clearCache() {
        cachedCodeVision.clear()
    }


    // todo: just calling DaemonCodeAnalyzer.restart does not force CodeVisionPass.
    // in the past it was possible to call CodeVisionPassFactory.clearModificationStamp(editor)
    // before DaemonCodeAnalyzer.restart and that worked.
    // but since CodeVisionPassFactory became internal its not possible to call
    // CodeVisionPassFactory.clearModificationStamp(editor).
    // after consulting the jetbrains forum i found the only code that works bellow.
    // but it seems a waste to clear and refresh all editors when we need to refresh only one file.
    // and also looks like something we don't want to do.
    // but it works and doesn't seem to cause any issues.
    // try to find a better standard way to force a refresh.
    fun refresh(psiFile: PsiFile) {
        clearCacheForFile(psiFile)
        EDT.ensureEDT {
            WriteAction.run<RuntimeException> {
                if (!project.isDisposed) {
                    val manager = PsiManager.getInstance(project)
                    manager.dropPsiCaches()
                    manager.dropResolveCaches()
                    DaemonCodeAnalyzer.getInstance(project).restart()
                }
            }
        }
    }

    fun refresh() {
        clearCache()
        EDT.ensureEDT {
            WriteAction.run<RuntimeException> {
                if (!project.isDisposed) {
                    val manager = PsiManager.getInstance(project)
                    manager.dropPsiCaches()
                    manager.dropResolveCaches()
                    DaemonCodeAnalyzer.getInstance(project).restart()
                }
            }
        }
    }

    private fun psiFileToKey(psiFile: PsiFile): String {
        return PsiUtils.psiFileToUri(psiFile)
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