package org.digma.intellij.plugin.idea.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.source.PsiExtensibleClass
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElementOfType


fun findMethodInClass(project: Project, cls: UClass, methodId: String): UMethod? {
    return getMethodsInClass(project, cls).firstOrNull { uMethod: UMethod -> methodId == createMethodCodeObjectId(uMethod) }
}

fun getMethodsInClass(project: Project, cls: UClass): Collection<UMethod> {

    if (cls.sourcePsi is PsiExtensibleClass) {

        // avoid cases when there are generated methods and/or constructors such as lombok creates,
        // see issue https://github.com/digma-ai/digma-intellij-plugin/issues/833
        // see issue https://youtrack.jetbrains.com/issue/IDEA-323198

        val ownMethods = if (ApplicationManager.getApplication().isReadAccessAllowed) {
            (cls.sourcePsi as PsiExtensibleClass).ownMethods
        } else {
            runInReadAccessWithResult(project) { (cls.sourcePsi as PsiExtensibleClass).ownMethods }
        }
        return ownMethods.map { psiMethod: PsiMethod -> psiMethod.toUElementOfType<UMethod>()!! }
    }
    return cls.methods.asList()
}

