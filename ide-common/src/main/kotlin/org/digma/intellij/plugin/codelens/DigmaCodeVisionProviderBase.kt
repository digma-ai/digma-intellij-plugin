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
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.psi.LanguageServiceProvider
import org.digma.intellij.plugin.psi.isSupportedLanguageFile

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


    private val empty: List<Pair<TextRange, CodeVisionEntry>> = listOf()


    override fun computeForEditor(editor: Editor, file: PsiFile): List<Pair<TextRange, CodeVisionEntry>> {

        if (file.project.isDefault) return empty
        if (!acceptsFile(file)) return empty

        //todo: maybe too long
        if (ProjectFileIndex.getInstance(file.project).isInLibrarySource(file.viewProvider.virtualFile)) return empty

        if(logger.isTraceEnabled){
            Log.log(logger::trace, "computeForEditor called for provider {} for file {}", id, file)
        }

        try {
            val project: Project = file.project

            val languageService = LanguageServiceProvider.getInstance(project).getLanguageService(file.language)
            if(languageService == null){
                if (logger.isTraceEnabled) {
                    Log.log(logger::trace,"Could not find language service for fle {}",file)
                }
                return empty
            }

            if (logger.isTraceEnabled) {
                Log.log(logger::trace, "found LanguageService for file {}, {}", file, languageService)
            }

            //not all languages support DaemonBoundCodeVisionProvider, C# does it in resharper
            if (languageService.isCodeVisionSupported()) {
                if (logger.isTraceEnabled) {
                    Log.log(logger::trace, "file is supported, computing code lens for {}", file)
                }
                return project.service<CodeLensService>().getCodeLens(this.id, file, languageService)
            } else {
                if (logger.isTraceEnabled) {
                    Log.log(logger::trace, "file is NOT supported,returning empty code lens list for {}", file)
                }
                return empty
            }

        } catch (pce: ProcessCanceledException) {
            //don't swallow or report ProcessCanceledException here; we have nothing to do about it,
            // the code vision infrastructure will retry
            throw pce
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(
                "DigmaCodeVisionProviderBase.computeForEditor", e, mapOf(
                    "provider id" to id
                )
            )
            return empty
        }
    }

    private fun acceptsFile(file: PsiFile): Boolean {
        return isSupportedLanguageFile(file.project, file)
    }

}