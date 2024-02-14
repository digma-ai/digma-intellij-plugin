package org.digma.intellij.plugin.idea.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.search.searches.MethodReferencesSearch
import org.digma.intellij.plugin.common.SearchScopeProvider
import org.digma.intellij.plugin.common.runInReadAccessInSmartModeWithResultAndRetryIgnorePCE


fun findAnnotatedMethods(project: Project, annotationClass: PsiClass, searchScope: SearchScopeProvider): List<SmartPsiElementPointer<PsiMethod>> {
    return runInReadAccessInSmartModeWithResultAndRetryIgnorePCE(project) {
        val psiMethodsQuery = AnnotatedElementsSearch.searchPsiMethods(annotationClass, searchScope.get())
        psiMethodsQuery.findAll().map { SmartPointerManager.getInstance(project).createSmartPsiElementPointer(it) }
    } ?: listOf()
}

fun findMethodReferences(project: Project, psiMethod: PsiMethod, searchScope: SearchScopeProvider): List<PsiReference> {
    return runInReadAccessInSmartModeWithResultAndRetryIgnorePCE(project) {
        val references = MethodReferencesSearch.search(psiMethod, searchScope.get(), true)
        references.findAll().toList()
    } ?: listOf()
}