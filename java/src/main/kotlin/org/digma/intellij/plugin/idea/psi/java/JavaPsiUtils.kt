package org.digma.intellij.plugin.idea.psi.java

import com.intellij.psi.PsiClass
import org.jetbrains.annotations.NotNull

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
    }
}