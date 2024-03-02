package org.digma.intellij.plugin.codelens

import com.intellij.codeInsight.codeVision.CodeVisionAnchorKind
import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering
import com.intellij.codeInsight.hints.codeVision.DaemonBoundCodeVisionProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.errorreporting.SEVERITY_HIGH_TRY_FIX
import org.digma.intellij.plugin.errorreporting.SEVERITY_PROP_NAME
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.psi.LanguageServiceLocator
import org.digma.intellij.plugin.psi.PsiUtils

abstract class DigmaCodeVisionProviderBase : DaemonBoundCodeVisionProvider {

    private val logger: Logger = Logger.getInstance(DigmaCodeVisionProviderBase::class.java)

    override val id: String
        get() = this::class.simpleName.toString()

    override val name: String
        get() = this::class.simpleName.toString()

    override val defaultAnchor: CodeVisionAnchorKind
        get() = CodeVisionAnchorKind.Top

    override val relativeOrderings: List<CodeVisionRelativeOrdering>
        get() = listOf(CodeVisionRelativeOrdering.CodeVisionRelativeOrderingAfter(LIVE_PROVIDER_ID))

    override val groupId: String
        get() = DIGMA_CODE_LENS_GROUP_ID


    open var myLensId: String? = null


    private val empty: List<Pair<TextRange, CodeVisionEntry>> = listOf()


    override fun computeForEditor(editor: Editor, file: PsiFile): List<Pair<TextRange, CodeVisionEntry>> {

        Log.log(logger::trace, "computeForEditor called for provider {} for file {}", id, file)

        try {
            val project: Project = editor.project ?: return empty

            if (editor.isDisposed || !PsiUtils.isValidPsiFile(file) || !isProjectValid(project)) {
                return empty
            }


            val languageService = LanguageServiceLocator.getInstance(project).locate(file.language)
            Log.log(logger::trace, "found LanguageService for file {}, {}", file, languageService)
            //not all languages support DaemonBoundCodeVisionProvider, C# does it in resharper
            if (languageService.isCodeVisionSupported) {
                Log.log(logger::trace, "file is supported, computing code lens for {}", file)
                return project.service<CodeLensService>().getCodeLens(this.id, file, languageService)
            } else {
                Log.log(logger::trace, "file is NOT supported,returning empty code lens list for {}", file)
                return empty
            }

        } catch (pce: ProcessCanceledException) {
            //don't swallow or report ProcessCanceledException here , we have nothing to do about it,
            // the code vision infrastructure will retry
            throw pce
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(
                "DigmaCodeVisionProviderBase.computeForEditor", e, mapOf(
                    SEVERITY_PROP_NAME to SEVERITY_HIGH_TRY_FIX,
                    "provider id" to id
                )
            )
            return empty
        }
    }

}