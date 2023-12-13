package org.digma.intellij.plugin.idea.psi.java

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import org.digma.intellij.plugin.instrumentation.CanInstrumentMethodResult
import org.digma.intellij.plugin.instrumentation.JvmCanInstrumentMethodResult
import org.digma.intellij.plugin.log.Log
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElementOfType

class JavaLanguageService(project: Project) : AbstractJavaLanguageService(project) {

    override fun findClassByClassName(className: String): UClass? {
        return JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.projectScope(project))?.toUElementOfType<UClass>()
//        val classes:Collection<PsiClass> = JavaFullClassNameIndex.getInstance().get(className, project, GlobalSearchScope.projectScope(project))
//        return classes.firstOrNull()?.toUElementOfType<UClass>()
    }


    override fun instrumentMethod(result: CanInstrumentMethodResult): Boolean {
        if (result !is JvmCanInstrumentMethodResult) {
            Log.log(logger::warn, "instrumentMethod was called with failing result from canInstrumentMethod")
            return false
        }


        if (result.containingFile.sourcePsi is PsiJavaFile && result.uMethod.sourcePsi is PsiMethod) {


            val psiJavaFile: PsiJavaFile = result.containingFile.sourcePsi as PsiJavaFile
            val psiMethod: PsiMethod = result.uMethod.sourcePsi as PsiMethod
            val methodId: String = result.methodId
            val withSpanClass: PsiClass = result.withSpanClass

            val importList = psiJavaFile.importList
            if (importList == null) {
                Log.log(logger::warn, "Failed to get ImportList from PsiFile (methodId: {})", methodId)
                return false
            }

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
            return false
        }

    }
}