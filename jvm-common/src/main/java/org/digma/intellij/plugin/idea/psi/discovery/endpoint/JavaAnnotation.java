package org.digma.intellij.plugin.idea.psi.discovery.endpoint;

import com.intellij.psi.PsiClass;

//package protected, not necessary in other packages
public class JavaAnnotation {

    private final String classNameFqn;
    private final PsiClass psiClass;

    public JavaAnnotation(String classNameFqn, PsiClass psiClass) {
        this.classNameFqn = classNameFqn;
        this.psiClass = psiClass;
    }

    public String getClassNameFqn() {
        return classNameFqn;
    }

    public PsiClass getPsiClass() {
        return psiClass;
    }
}
