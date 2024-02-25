package org.digma.intellij.plugin.idea.psi.java

import com.intellij.codeInsight.hints.InlayHintsUtils
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import org.digma.intellij.plugin.codelens.AbstractCodeLensService
import org.digma.intellij.plugin.common.runInReadAccessWithResult
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.psi.createMethodCodeObjectId
import org.digma.intellij.plugin.psi.PsiUtils
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElementOfType


@Suppress("LightServiceMigrationCode")
class JavaCodeLensService(project: Project): AbstractCodeLensService(project) {


    override fun findMethodsByCodeObjectIds(psiFile: PsiFile, ids: Set<String>): Map<String, Pair<TextRange,PsiElement>> {

        if (ids.isEmpty() || !PsiUtils.isValidPsiFile(psiFile)) {
            return emptyMap()
        }

        try {

            return runInReadAccessWithResult {
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

                return@runInReadAccessWithResult methods
            }
        } catch (pce: ProcessCanceledException) {
            //don't swallow or report ProcessCanceledException here , we have nothing to do about it,
            // the code vision infrastructure will retry
            throw pce
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("JavaCodeLensService.findMethodsByCodeObjectIds", e)
            return mapOf()
        }
    }
}