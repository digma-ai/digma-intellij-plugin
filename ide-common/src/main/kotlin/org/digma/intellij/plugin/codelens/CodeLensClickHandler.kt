package org.digma.intellij.plugin.codelens

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.objectToJsonNode
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.lens.CodeLens
import org.digma.intellij.plugin.notifications.NotificationUtil
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.scope.ScopeContext
import org.digma.intellij.plugin.scope.ScopeManager
import org.digma.intellij.plugin.scope.SpanScope

internal class CodeLensClickHandler(
    private val project: Project,
    private val lens: CodeLens,
    private val elementPointer: SmartPsiElementPointer<PsiElement>
) {

    private val logger: Logger = Logger.getInstance(this::class.java)

    fun handle() {

        try {
            ActivityMonitor.getInstance(project).registerLensClicked(lens.id)
            elementPointer.element?.let {
                if (it is Navigatable && it.canNavigateToSource()) {
                    it.navigate(true)
                } else {
                    //it's a fallback. sometimes the psiMethod.canNavigateToSource is false and really the
                    //navigation doesn't work. I can't say why. usually it happens when indexing is not ready yet,
                    // and the user opens files, selects tabs or moves the caret. then when indexing is finished,
                    // we have the list of methods but then psiMethod.navigate doesn't work.
                    // navigation to source using the editor does work in these circumstances.
                    val selectedEditor = FileEditorManager.getInstance(project).selectedTextEditor
                    selectedEditor?.caretModel?.moveToOffset(it.textOffset)
                }
            }

            Backgroundable.ensurePooledThreadWithoutReadAccess {
                val isErrorHotspot = lens.lensTitle.lowercase().contains("error")
                val scopeCodeObjectId = lens.scopeCodeObjectId
                val contextPayload = objectToJsonNode(ChangeScopeMessagePayload(lens))
                val scopeContext = ScopeContext("IDE/CODE_LENS_CLICKED", contextPayload)
                if (isErrorHotspot) {
                    ScopeManager.getInstance(project)
                        .changeToHome(true, scopeContext, null)
                }else{
                    if (scopeCodeObjectId == null) {
                        EDT.ensureEDT {
                            NotificationUtil.notifyFadingInfo(project, "No asset found for method: ${lens.codeMethod}")
                        }
                    }else{
                        ScopeManager.getInstance(project)
                            .changeScope(SpanScope(spanCodeObjectId = scopeCodeObjectId, methodId = lens.codeMethod), scopeContext, null)
                    }
                }
            }
        } catch (e: Exception) {
            Log.warnWithException(logger, project, e, "error in CodeLensClickHandler {}", e)
            ErrorReporter.getInstance().reportError(project, "CodeLensClickHandler.handle", e)
        }
    }
}