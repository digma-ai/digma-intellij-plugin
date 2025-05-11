package org.digma.intellij.plugin.idea.psi.kotlin

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.concurrency.annotations.RequiresEdt
import kotlinx.coroutines.CancellationException
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.psi.AbstractJvmInstrumentationProvider
import org.digma.intellij.plugin.instrumentation.MethodObservabilityInfo
import org.digma.intellij.plugin.log.Log
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getContainingUFile
import java.util.Objects

class KotlinInstrumentationProvider(project: Project, kotlinLanguageService: KotlinLanguageService) :
    AbstractJvmInstrumentationProvider(project, kotlinLanguageService) {

    @RequiresEdt
    override suspend fun instrumentMethod(methodObservabilityInfo: MethodObservabilityInfo): Boolean {

        try {

            if (methodObservabilityInfo.hasMissingDependency || methodObservabilityInfo.annotationClassFqn == null) {
                Log.log(logger::warn, "instrumentMethod was called with failing result from canInstrumentMethod")
                return false
            }

            val uMethod = languageService.findUMethodByMethodCodeObjectId(methodObservabilityInfo.methodId)
            //will be caught here so that ErrorReporter will report it
            Objects.requireNonNull(uMethod, "can't instrument method,can't find psi method for ${methodObservabilityInfo.methodId}")

            uMethod as UMethod

            val ktFile = uMethod.getContainingUFile()?.sourcePsi
            val ktFunction = uMethod.sourcePsi
            val annotationFqn = methodObservabilityInfo.annotationClassFqn


            if (ktFile is KtFile && ktFunction is KtFunction && annotationFqn != null) {

                val withSpanClass = JavaPsiFacade.getInstance(project).findClass(annotationFqn, GlobalSearchScope.allScope(project))
                //will be caught here so that ErrorReporter will report it
                Objects.requireNonNull(
                    withSpanClass,
                    "can't instrument method,can't find annotation class  ${methodObservabilityInfo.annotationClassFqn}"
                )

                withSpanClass as PsiClass

                val importList = ktFile.importList
                //will be caught here so that ErrorReporter will report it
                Objects.requireNonNull(importList, "Failed to get ImportList from PsiFile (methodId: ${methodObservabilityInfo.methodId})")

                importList as KtImportList

                WriteCommandAction.runWriteCommandAction(project) {
                    val ktPsiFactory = KtPsiFactory(project)
                    val shortClassNameAnnotation = withSpanClass.name
                    if (shortClassNameAnnotation != null) {
                        ktFunction.addAnnotationEntry(ktPsiFactory.createAnnotationEntry("@$shortClassNameAnnotation"))
                    }

                    val existing =
                        importList.imports.find { ktImportDirective: KtImportDirective? -> ktImportDirective?.importedFqName?.asString() == withSpanClass.qualifiedName }
                    if (existing == null) {
                        val importStatement = ktPsiFactory.createImportDirective(ImportPath.fromString(withSpanClass.qualifiedName!!))
                        importList.add(importStatement)
                    }
                }
                return true
            } else {
                Log.warn(logger, "instrumentMethod failed to find psi file or psi function for methodId:{}", methodObservabilityInfo.methodId)
                return false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "KotlinLanguageService.instrumentMethod", e)
            return false
        }
    }
}