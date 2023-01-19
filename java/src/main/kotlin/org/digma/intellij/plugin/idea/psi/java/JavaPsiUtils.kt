package org.digma.intellij.plugin.idea.psi.java

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

class JavaPsiUtils {

    companion object {
        private const val OBJECT_CLASS_FQN = "java.lang.Object"

        @JvmStatic
        fun isBaseClass(psiClass: PsiClass): Boolean {
            val superClass = psiClass.superClass
            return (superClass == null || superClass.qualifiedName.equals(OBJECT_CLASS_FQN))
        }

        /**
         * Climbs up from this class to its super classes and searches for the first class that is after the java.lang.Object.
         */
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

        @JvmStatic
        @NotNull
        fun getClassesWithin(psiFile: PsiFile): List<PsiClass> {
            //it must be a PsiJavaFile so casting should be ok
            val psiJavaFile = psiFile as PsiJavaFile
            return psiJavaFile.classes.asList()
        }

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

    }
}