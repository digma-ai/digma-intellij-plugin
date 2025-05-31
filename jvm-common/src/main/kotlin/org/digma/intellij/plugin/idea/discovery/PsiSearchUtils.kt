package org.digma.intellij.plugin.idea.discovery

import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import org.digma.intellij.plugin.common.SearchScopeProvider

suspend fun findAnnotatedMethods(
    project: Project,
    annotationClassPointer: SmartPsiElementPointer<PsiClass>,
    searchScope: SearchScopeProvider,
): List<SmartPsiElementPointer<PsiMethod>> {
    return smartReadAction(project) {
        annotationClassPointer.element?.let { annotationClass ->
            val psiMethodsQuery = AnnotatedElementsSearch.searchPsiMethods(annotationClass, searchScope.get())
            psiMethodsQuery.findAll().map { SmartPointerManager.getInstance(project).createSmartPsiElementPointer(it) }
        }
    } ?: listOf()
}

//fun findMethodReferences(
//    project: Project,
//    psiMethodPointer: SmartPsiElementPointer<PsiMethod>,
//    searchScope: SearchScopeProvider,
//): List<PsiReference> {
//    return runInReadAccessInSmartModeWithResultAndRetryIgnorePCE(project) {
//        psiMethodPointer.element?.let { psiMethod ->
//            val references = MethodReferencesSearch.search(psiMethod, searchScope.get(), true)
//            references.findAll().toList()
//        }
//    } ?: listOf()
//}