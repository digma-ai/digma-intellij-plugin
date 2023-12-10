package org.digma.intellij.plugin.idea.psi.java

import com.intellij.psi.PsiClass

data class JavaAnnotation(
    val classNameFqn: String,
    val psiClass: PsiClass,
)