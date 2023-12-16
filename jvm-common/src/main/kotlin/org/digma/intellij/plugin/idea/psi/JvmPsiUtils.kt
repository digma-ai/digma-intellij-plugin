package org.digma.intellij.plugin.idea.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.source.PsiExtensibleClass
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.idea.psi.java.JavaPsiUtils
import org.digma.intellij.plugin.log.Log
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElementOfType


fun findMethodInClass(project: Project, cls: UClass, methodId: String): UMethod? {
    return getMethodsInClass(project, cls).filter { uMethod: UMethod -> methodId == createMethodCodeObjectId(uMethod) }.firstOrNull()
}

fun getMethodsInClass(project: Project, cls: UClass): Collection<UMethod> {

    if (cls.sourcePsi is PsiExtensibleClass) {

        // avoid cases when there are generated methods and/or constructors such as lombok creates,
        // see issue https://github.com/digma-ai/digma-intellij-plugin/issues/833
        // see issue https://youtrack.jetbrains.com/issue/IDEA-323198

        val ownMethods = if (ApplicationManager.getApplication().isReadAccessAllowed) {
            (cls.sourcePsi as PsiExtensibleClass).ownMethods
        } else {
            JavaPsiUtils.runInReadAccessWithResult(project) { (cls.sourcePsi as PsiExtensibleClass).ownMethods }
        }
        return ownMethods.map { psiMethod: PsiMethod -> psiMethod.toUElementOfType<UMethod>()!! }
    }
    return cls.methods.asList()
}

