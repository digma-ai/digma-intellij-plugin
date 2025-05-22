package org.digma.intellij.plugin.codelens

import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.hints.InlayHintsUtils
import com.intellij.codeInsight.hints.codeVision.ModificationStampUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPointerManager
import io.ktor.util.collections.ConcurrentMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.digma.intellij.plugin.codelens.provider.CodeLensChanged
import org.digma.intellij.plugin.codelens.provider.CodeLensProvider
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.objectToJsonNode
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.kotlin.ext.launchWithErrorReporting
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.lens.CodeLens
import org.digma.intellij.plugin.notifications.NotificationUtil
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.psi.LanguageService
import org.digma.intellij.plugin.scope.ScopeContext
import org.digma.intellij.plugin.scope.ScopeManager
import org.digma.intellij.plugin.scope.SpanScope
import java.awt.event.MouseEvent
import kotlin.coroutines.cancellation.CancellationException

//don't convert to light service because it will register on all IDEs, but we want it only on Idea and Pycharm
@Suppress("LightServiceMigrationCode")
class CodeLensService(private val project: Project, private val cs: CoroutineScope) : Disposable {

    private val logger: Logger = Logger.getInstance(this::class.java)
    private val runningJobs: ConcurrentMap<VirtualFile, Job> = ConcurrentMap()

    class CodeLensServiceCodelensChangeListener(private val project: Project) : CodeLensChanged {
        override fun codelensChanged(virtualFile: VirtualFile) {
            project.service<CodeLensService>().refreshFile(virtualFile)
        }

        override fun codelensRemoved(virtualFile: VirtualFile) {
            project.service<CodeLensService>().refreshFile(virtualFile)
        }
    }

    override fun dispose() {
        runningJobs.values.forEach { it.cancel(CancellationException("CodeLensService is disposing")) }
        runningJobs.clear()
    }

    fun getCodeLens(providerId: String, psiFile: PsiFile, languageService: LanguageService): List<Pair<TextRange, CodeVisionEntry>> {

        if (logger.isTraceEnabled) {
            Log.log(logger::trace, "getCodeLens called for provider {} for file {}", providerId, psiFile)
        }

        val virtualFile = psiFile.virtualFile ?: return emptyList()

        val codeLensesForProvider = selectCodeLensesForProvider(providerId, virtualFile)
        if (logger.isTraceEnabled) {
            Log.log(logger::trace, "got codeLensesForProvider for provider {} for file {} [{}]", providerId, psiFile, codeLensesForProvider)
        }

        val codeLensContainer = CodeLensContainer()

        codeLensesForProvider?.let { lenses ->

            val methodIds = lenses.map { it.codeMethod }
            val methodsPsiElements: Map<String, PsiElement> = languageService.findMethodsByCodeObjectIds(psiFile, methodIds)

            lenses.forEach { lens ->

                if (logger.isTraceEnabled) {
                    Log.log(logger::trace, "adding code vision for provider {} for file {} [{}]", providerId, psiFile, lens)
                }

                val psiMethod = methodsPsiElements[lens.codeMethod]
                psiMethod?.let { method ->

                    val entry = ClickableTextCodeVisionEntry(
                        lens.lensTitle,
                        providerId,
                        ClickHandler(method, lens, project),
                        null, // icon was set already on a previous step inside CodeLensProvider.buildCodeLens()
                        lens.lensMoreText,
                        lens.lensDescription
                    )

                    @Suppress("UnstableApiUsage")
                    val textRange = InlayHintsUtils.getTextRangeWithoutLeadingCommentsAndWhitespaces(method)
                    codeLensContainer.codeLensList.add(Pair(textRange, entry))
                }
            }
        }

        return codeLensContainer.codeLensList
    }


    private fun selectCodeLensesForProvider(providerId: String, virtualFile: VirtualFile): Set<CodeLens>? {

        val codeLensesForFile = CodeLensProvider.getInstance(project).getCodeLens(virtualFile)

        val providerLensSelector = project.service<CodeVisionProviderToLensSelector>()

        //each DigmaCodeVisionProviderBase provides a lens for the same lens id.
        //CodeVisionProviderToLensSelector will always return the same lens id for this provider.
        //it is a random per session/project selection, but makes sure this provider will always
        // provide for the same lens id for this project in this IDE session.
        //if this provider already assigned a lens id, it is returned; otherwise the next unassigned
        // lens id will be selected.
        //if all providers are already assigned a lens id and there are more lens types than providers,
        // then some lens types will not be shown.
        //there are 15 providers, that should be enough. if there are more than 15 lens types, then
        // we need more providers.
        val providerLensId = providerLensSelector.selectLensForProvider(providerId, codeLensesForFile)

        return providerLensId?.let { lensId ->
            if (logger.isTraceEnabled) {
                Log.log(logger::trace, "selected code lens {} for provider {}", lensId, providerId)
            }
            codeLensesForFile.filter { it.id == lensId }.toSet()
        }
    }


    private fun refreshFile(virtualFile: VirtualFile) {
        if (logger.isTraceEnabled) {
            Log.log(logger::trace, "starting refreshFile job for {}", virtualFile)
        }
        runningJobs[virtualFile]?.cancel(CancellationException("CodeLensService.refreshFile called again before previous job finished"))
        val job = cs.launchWithErrorReporting("CodeLensService.refreshFile", logger) {
            val editor =
                FileEditorManager.getInstance(project).getEditors(virtualFile).firstOrNull { editor -> editor is TextEditor } as? TextEditor
                    ?: return@launchWithErrorReporting
            ModificationStampUtil.clearModificationStamp(editor.editor)
            val psiFile = readAction {
                PsiManager.getInstance(project).findFile(virtualFile)
            }
            psiFile?.let {
                if (logger.isTraceEnabled) {
                    Log.log(logger::trace, "calling DaemonCodeAnalyzer.restart for {}", virtualFile)
                }
                DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
            }
        }
        runningJobs[virtualFile] = job
        job.invokeOnCompletion { cause ->
            runningJobs.remove(virtualFile)
        }
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
                        //it's a fallback. sometimes the psiMethod.canNavigateToSource is false, and really the
                        //navigation doesn't work. I can't say why. usually it happens when indexing is not ready yet,
                        // and the user opens files, selects tabs or moves the caret. then when indexing is finished,
                        // we have the list of methods, but then psiMethod.navigate doesn't work.
                        // navigation to source using the editor does work in these circumstances.
                        val selectedEditor = FileEditorManager.getInstance(project).selectedTextEditor
                        selectedEditor?.caretModel?.moveToOffset(it.textOffset)
                    }
                }

                val scopeCodeObjectId = lens.scopeCodeObjectId
                if (scopeCodeObjectId == null) {
                    NotificationUtil.notifyFadingInfo(project, "No asset found for method: ${lens.codeMethod}")
                } else {
                    Backgroundable.ensurePooledThreadWithoutReadAccess {
                        val contextPayload = objectToJsonNode(ChangeScopeMessagePayload(lens))
                        val scopeContext = ScopeContext("IDE/CODE_LENS_CLICKED", contextPayload)
                        ScopeManager.getInstance(project).changeScope(SpanScope(scopeCodeObjectId), scopeContext, null)
                    }
                }
            } catch (e: Exception) {
                Log.warnWithException(logger, project, e, "error in ClickHandler {}", e)
                ErrorReporter.getInstance().reportError(project, "${this::class.simpleName}.ClickHandler.invoke", e)
            }
        }
    }
}