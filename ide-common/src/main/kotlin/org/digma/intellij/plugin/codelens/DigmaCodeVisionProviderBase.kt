package org.digma.intellij.plugin.codelens

import com.intellij.codeInsight.codeVision.CodeVisionAnchorKind
import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering
import com.intellij.codeInsight.hints.codeVision.DaemonBoundCodeVisionProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.psi.LanguageService
import org.digma.intellij.plugin.psi.LanguageServiceLocator

abstract class DigmaCodeVisionProviderBase: DaemonBoundCodeVisionProvider {

    private val logger: Logger = Logger.getInstance(DigmaCodeVisionProviderBase::class.java)

    internal val empty: List<Pair<TextRange, CodeVisionEntry>> = listOf()

    override val defaultAnchor: CodeVisionAnchorKind
        get() = CodeVisionAnchorKind.Top

    override val relativeOrderings: List<CodeVisionRelativeOrdering>
        get() = listOf(CodeVisionRelativeOrdering.CodeVisionRelativeOrderingFirst)




    override fun computeForEditor(editor: Editor, file: PsiFile): List<Pair<TextRange, CodeVisionEntry>> {

        Log.log(logger::debug,"got request to computeForEditor code lens for {}",file.virtualFile)

        val project: Project = editor.project ?: return emptyList()

        val languageService = LanguageServiceLocator.getInstance(project).locate(file.language)
        //not all languages support DaemonBoundCodeVisionProvider, C# does it in resharper
        if (languageService.isCodeVisionSupported){
            Log.log(logger::debug,"file is supported, computing code lens for {}",file.virtualFile)
            return computeLenses(editor, file,languageService)
        }

        return emptyList()
    }

    private fun computeLenses(editor: Editor, psiFile: PsiFile, languageService: LanguageService): List<Pair<TextRange, CodeVisionEntry>> {
        if (psiFile.virtualFile == null) {
            return empty
        }
        editor.project?.let {
            if (languageService.isCodeVisionSupported){
                return languageService.getCodeLens(psiFile)
            }
        }
        return emptyList()
    }

}