package org.digma.intellij.plugin.idea.psi.java

import com.intellij.codeInsight.hints.InlayHintsUtils
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.codelens.AbstractCodeLensService
import org.digma.intellij.plugin.idea.frameworks.SpringBootMicrometerConfigureDepsService
import org.digma.intellij.plugin.posthog.ActivityMonitor


class JavaCodeLensService(project: Project): AbstractCodeLensService(project) {

    companion object {
        private val logger = Logger.getInstance(JavaCodeLensService::class.java)
        @JvmStatic
        fun getInstance(project: Project): JavaCodeLensService {
            logger.warn("Getting instance of ${JavaCodeLensService::class.simpleName}")
            return project.getService(JavaCodeLensService::class.java)
        }
    }



    override fun findMethodsByCodeObjectIds(psiFile: PsiFile, ids: Set<String>): Map<String, Pair<TextRange,PsiElement>> {

        if (ids.isEmpty()){
            return emptyMap()
        }

        return ReadAction.compute<Map<String, Pair<TextRange,PsiMethod>>,Exception> {
            val methods = mutableMapOf<String, Pair<TextRange,PsiMethod>>()
            val traverser = SyntaxTraverser.psiTraverser(psiFile)
            for (element in traverser) {
                if (element is PsiMethod) {
                    val codeObjectId = JavaLanguageUtils.createJavaMethodCodeObjectId(element)
                    if (ids.contains(codeObjectId)) {
                        @Suppress("UnstableApiUsage")
                        val textRange = InlayHintsUtils.getTextRangeWithoutLeadingCommentsAndWhitespaces(element)
                        methods[codeObjectId] = Pair(textRange,element)
                    }
                }
            }

            return@compute methods
        }
    }


}