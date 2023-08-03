package org.digma.intellij.plugin.psi.python

import com.intellij.codeInsight.hints.InlayHintsUtils
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SyntaxTraverser
import com.jetbrains.python.psi.PyFunction
import org.digma.intellij.plugin.codelens.AbstractCodeLensService
import org.digma.intellij.plugin.ui.service.InsightsViewService

class PythonCodeLensService(project: Project): AbstractCodeLensService(project) {

     companion object {
         private val logger = Logger.getInstance(PythonCodeLensService::class.java)
         @JvmStatic
        fun getInstance(project: Project): PythonCodeLensService {
            logger.warn("Getting instance of ${PythonCodeLensService::class.simpleName}")
            return project.getService(PythonCodeLensService::class.java)
        }
    }
    override fun findMethodsByCodeObjectIds(psiFile: PsiFile, ids: Set<String>): Map<String, Pair<TextRange, PsiElement>> {

        if (ids.isEmpty()){
            return emptyMap()
        }

        return ReadAction.compute<Map<String, Pair<TextRange,PsiElement>>,Exception> {
            val methods = mutableMapOf<String, Pair<TextRange,PsiElement>>()
            val traverser = SyntaxTraverser.psiTraverser(psiFile)
            for (element in traverser) {
                if (element is PyFunction) {
                    val codeObjectId = PythonLanguageUtils.createPythonMethodCodeObjectId(psiFile.project, element)
                    val methodIds = PythonAdditionalIdsProvider.getAdditionalIdsInclusive(codeObjectId,false)
                    if (ids.intersect(methodIds.toSet()).any()) {
                        @Suppress("UnstableApiUsage")
                        val textRange = InlayHintsUtils.getTextRangeWithoutLeadingCommentsAndWhitespaces(element)
                        methods[codeObjectId] = Pair(textRange, element)
                    }
                }
            }

            return@compute methods
        }
    }

}