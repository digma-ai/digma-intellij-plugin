package org.digma.intellij.plugin.idea.psi.java

import com.intellij.codeInsight.hints.InlayHintsUtils
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import org.digma.intellij.plugin.codelens.AbstractCodeLensService
import org.digma.intellij.plugin.idea.psi.createMethodCodeObjectId
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElementOfType


@Suppress("LightServiceMigrationCode")
class JavaCodeLensService(project: Project): AbstractCodeLensService(project) {


    override fun findMethodsByCodeObjectIds(psiFile: PsiFile, ids: Set<String>): Map<String, Pair<TextRange,PsiElement>> {

        if (ids.isEmpty()){
            return emptyMap()
        }

        return ReadAction.compute<Map<String, Pair<TextRange, PsiElement>>, Exception> {
            val methods = mutableMapOf<String, Pair<TextRange, PsiElement>>()

            val visitor = object : JavaRecursiveElementWalkingVisitor() {

                override fun visitMethod(method: PsiMethod) {
                    if (method.toUElementOfType<UMethod>() != null) {
                        val codeObjectId = createMethodCodeObjectId(method.toUElementOfType<UMethod>()!!)
                        if (ids.contains(codeObjectId)) {
                            @Suppress("UnstableApiUsage")
                            val textRange = InlayHintsUtils.getTextRangeWithoutLeadingCommentsAndWhitespaces(method)
                            methods[codeObjectId] = Pair(textRange, method)
                        }
                    }
                }
            }

            psiFile.acceptChildren(visitor)

            return@compute methods
        }
    }
}