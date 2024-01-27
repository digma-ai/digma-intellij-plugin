package org.digma.intellij.plugin.idea.psi.java

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.source.PsiExtensibleClass
import com.intellij.psi.util.PsiTreeUtil
import org.digma.intellij.plugin.idea.psi.runInReadAccessInSmartModeWithResult
import org.digma.intellij.plugin.psi.PsiUtils
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable


class JavaPsiUtils {

    companion object {
        private const val OBJECT_CLASS_FQN = "java.lang.Object"


        //must be called with read access
        @JvmStatic
        fun isBaseClass(psiClass: PsiClass): Boolean {
            val superClass = psiClass.superClass
            return (superClass == null || superClass.qualifiedName.equals(OBJECT_CLASS_FQN))
        }

        /**
         * Climbs up from this class to its super classes and searches for the first class that is after the java.lang.Object.
         */
        //must be called with read access
        @JvmStatic
        @NotNull
        fun climbUpToBaseClass(psiClass: PsiClass): PsiClass {
            var prevCLass: PsiClass = psiClass
            var currentClass: PsiClass? = psiClass
            while (currentClass != null && !isBaseClass(currentClass)) {
                prevCLass = currentClass
                currentClass = currentClass.superClass // recursion
            }

            if (currentClass != null) {
                return currentClass
            } else {
                return prevCLass
            }
        }

        //must be called with read access
        @JvmStatic
        @Nullable
        fun findNearestAnnotation(psiMethod: PsiMethod, annotationFqn: String): PsiAnnotation? {
            val annotClass = psiMethod.getAnnotation(annotationFqn)
            if (annotClass != null) {
                return annotClass
            }

            val superMethods = psiMethod.findSuperMethods(false)
            superMethods.forEach {
                val theAnnotation = it.getAnnotation(annotationFqn)
                if (theAnnotation != null) {
                    return theAnnotation
                }
            }
            return null
        }

        @JvmStatic
        @Nullable
        fun findNearestAnnotation(psiClass: PsiClass, annotationFqn: String): PsiAnnotation? {
            val annotClass = psiClass.getAnnotation(annotationFqn)
            if (annotClass != null) {
                return annotClass
            }

            val superClasses = psiClass.supers
            superClasses.forEach {
                val theAnnotation = it.getAnnotation(annotationFqn)
                if (theAnnotation != null) {
                    return theAnnotation
                }
            }
            return null
        }


        @JvmStatic
        fun toFileUri(psiMethod: PsiMethod): String {
            val containingFile: PsiFile? = PsiTreeUtil.getParentOfType(psiMethod, PsiFile::class.java)
            var containingFileUri = PsiUtils.psiFileToUri(containingFile!!)
            return containingFileUri
        }


        @JvmStatic
        fun getMethodsOf(project: Project, psiClass: PsiClass): List<PsiMethod> {
            if (psiClass is PsiExtensibleClass) {

                // avoid cases when there are generated methods and/or constructors such as lombok creates,
                // see issue https://github.com/digma-ai/digma-intellij-plugin/issues/833
                // see issue https://youtrack.jetbrains.com/issue/IDEA-323198

                return if (ApplicationManager.getApplication().isReadAccessAllowed) {
                    psiClass.ownMethods
                } else {
                    runInReadAccessInSmartModeWithResult(project) { psiClass.ownMethods }
                }

            }
            return psiClass.methods.asList()
        }

        @JvmStatic
        fun getInnerClassesOf(psiClass: PsiClass): List<PsiClass> {
            if (psiClass is PsiExtensibleClass) {
                return psiClass.ownInnerClasses
            }
            return psiClass.innerClasses.asList()
        }




        @JvmStatic
        fun hasOneOfAnnotations(psiClass: PsiClass, vararg annotationsFqn: String): Boolean {
            annotationsFqn.forEach {
                val annotObj = psiClass.getAnnotation(it)
                if (annotObj != null) {
                    return true
                }
            }
            return false
        }

    }

}
