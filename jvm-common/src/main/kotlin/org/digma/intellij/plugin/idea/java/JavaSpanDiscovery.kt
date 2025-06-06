@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.idea.java

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import org.digma.intellij.plugin.idea.discovery.span.AbstractSpanDiscovery
import org.digma.intellij.plugin.idea.psi.java.JavaSpanDiscoveryUtils
import org.digma.intellij.plugin.model.discovery.SpanInfo
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UReferenceExpression

class JavaSpanDiscovery : AbstractSpanDiscovery() {

    //todo: refactor to have the same code for java and kotlin
    override fun findSpanInfosFromWithSpanAnnotatedMethod(uMethod: UMethod): List<SpanInfo> {
        //it should always be PsiMethod
        if (uMethod.sourcePsi is PsiMethod) {
            val psiMethod = uMethod.sourcePsi as PsiMethod
            return JavaSpanDiscoveryUtils.getSpanInfoFromWithSpanAnnotatedMethod(psiMethod)
        } else {
            //this actually should not happen
            return JavaSpanDiscoveryUtils.getSpanInfoFromWithSpanAnnotatedMethod(uMethod)
        }
    }

    //todo: refactor to have the same code for java and kotlin
    override fun findSpanInfoFromStartSpanMethodReference(project: Project, uReferenceExpression: UReferenceExpression): SpanInfo? {
        //if uReferenceExpression.sourcePsi is not PsiReference, this will throw an exception. but it should be
        val psiReference = uReferenceExpression.sourcePsi as PsiReference
        return JavaSpanDiscoveryUtils.getSpanInfoFromStartSpanMethodReference(project, psiReference)
    }


}