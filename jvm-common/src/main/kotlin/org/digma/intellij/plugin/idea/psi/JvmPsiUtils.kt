package org.digma.intellij.plugin.idea.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.source.PsiExtensibleClass
import org.digma.intellij.plugin.psi.LanguageService
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElementOfType


fun isJvmSupportedFile(project: Project, psiFile: PsiFile): Boolean {
    SupportedJvmLanguages.values().forEach {
        val languageService = LanguageService.findLanguageServiceByName(project, it.language.languageServiceClassName)
        if (languageService != null &&
            languageService is JvmLanguageService &&
            languageService.isSupportedFile(psiFile)
        ) {

            return true
        }
    }
    return false
}


fun getClassSimpleName(uClass: UClass): String {

    val packageName = uClass.getParentOfType<UFile>()?.packageName ?: ""
    val packageNameLength = if (packageName.isBlank()) {
        0
    } else {
        packageName.length + 1
    }

    return uClass.qualifiedName?.substring(packageNameLength) ?: uClass.name ?: ""
}


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

