package org.digma.intellij.plugin.idea.psi.java

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiImportList
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.concurrency.annotations.RequiresEdt
import kotlinx.coroutines.CancellationException
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.psi.AbstractJvmInstrumentationProvider
import org.digma.intellij.plugin.instrumentation.MethodObservabilityInfo
import org.digma.intellij.plugin.log.Log
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getContainingUFile
import java.util.Objects

class JavaInstrumentationProvider(project: Project, javaLanguageService: JavaLanguageService) :
    AbstractJvmInstrumentationProvider(project, javaLanguageService) {

    @RequiresEdt
    override suspend fun instrumentMethod(methodObservabilityInfo: MethodObservabilityInfo): Boolean {
        try {
            if (methodObservabilityInfo.hasMissingDependency || methodObservabilityInfo.annotationClassFqn == null) {
                Log.warn(logger, "instrumentMethod was called with failing result from methodObservabilityInfo")
                return false
            }

            val uMethod = languageService.findUMethodByMethodCodeObjectId(methodObservabilityInfo.methodId)
            //will be caught here so that ErrorReporter will report it
            Objects.requireNonNull(uMethod, "can't instrument method,can't find psi method for ${methodObservabilityInfo.methodId}")

            uMethod as UMethod


            val psiJavaFile = uMethod.getContainingUFile()?.sourcePsi
            val psiMethod = uMethod.sourcePsi
            val annotationFqn = methodObservabilityInfo.annotationClassFqn

            if (psiJavaFile is PsiJavaFile && psiMethod is PsiMethod && annotationFqn != null) {

                val withSpanClass = JavaPsiFacade.getInstance(project).findClass(annotationFqn, GlobalSearchScope.allScope(project))
                //will be caught here so that ErrorReporter will report it
                Objects.requireNonNull(
                    withSpanClass,
                    "can't instrument method,can't find annotation class  ${methodObservabilityInfo.annotationClassFqn}"
                )

                withSpanClass as PsiClass

                val importList = psiJavaFile.importList
                //will be caught here so that ErrorReporter will report it
                Objects.requireNonNull(importList, "Failed to get ImportList from PsiFile (methodId: ${methodObservabilityInfo.methodId})")

                importList as PsiImportList

                WriteCommandAction.runWriteCommandAction(project) {
                    val psiFactory = PsiElementFactory.getInstance(project)
                    val shortClassNameAnnotation = withSpanClass.name
                    if (shortClassNameAnnotation != null) {
                        psiMethod.modifierList.addAnnotation(shortClassNameAnnotation)
                    }

                    val existing = importList.findSingleClassImportStatement(withSpanClass.qualifiedName)
                    if (existing == null) {
                        val importStatement = psiFactory.createImportStatement(withSpanClass)
                        importList.add(importStatement)
                    }
                }
                return true
            } else {
                Log.warn(logger, "instrumentMethod failed to find psiJavaFile or psiMethod or annotationClassFqn")
                return false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "JavaInstrumentationProvider.instrumentMethod", e)
            return false
        }
    }
}