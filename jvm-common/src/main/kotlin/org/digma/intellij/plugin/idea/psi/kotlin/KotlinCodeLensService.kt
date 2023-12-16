package org.digma.intellij.plugin.idea.psi.kotlin

import com.intellij.codeInsight.hints.InlayHintsUtils
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.digma.intellij.plugin.codelens.AbstractCodeLensService
import org.digma.intellij.plugin.idea.psi.createMethodCodeObjectId
import org.jetbrains.kotlin.idea.structuralsearch.visitor.KotlinRecursiveElementWalkingVisitor
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElementOfType

@Suppress("LightServiceMigrationCode")
class KotlinCodeLensService(project: Project) : AbstractCodeLensService(project) {


    override fun findMethodsByCodeObjectIds(psiFile: PsiFile, ids: Set<String>): Map<String, Pair<TextRange, PsiElement>> {


        if (ids.isEmpty()) {
            return emptyMap()
        }

        return ReadAction.compute<Map<String, Pair<TextRange, PsiElement>>, Exception> {
            val methods = mutableMapOf<String, Pair<TextRange, PsiElement>>()

            val visitor = object : KotlinRecursiveElementWalkingVisitor() {

                override fun visitNamedFunction(function: KtNamedFunction) {
                    if (function.toUElementOfType<UMethod>() != null) {
                        val codeObjectId = createMethodCodeObjectId(function.toUElementOfType<UMethod>()!!)
                        if (ids.contains(codeObjectId)) {
                            @Suppress("UnstableApiUsage")
                            val textRange = InlayHintsUtils.getTextRangeWithoutLeadingCommentsAndWhitespaces(function)
                            methods[codeObjectId] = Pair(textRange, function)
                        }
                    }
                }
            }

            psiFile.acceptChildren(visitor)

            return@compute methods
        }

    }
}