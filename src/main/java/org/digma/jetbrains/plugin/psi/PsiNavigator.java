package org.digma.jetbrains.plugin.psi;

import com.intellij.psi.*;
import com.intellij.psi.util.*;

/**
 * Query and navigate PSI elements.
 * This is the only class dealing with language specific elements.
 */
public class PsiNavigator {

    //todo: handle all possible languages

    public static boolean isMethod(PsiElement psiElement) {
        return getMethod(psiElement) != null;
    }

    public static PsiElement getMethod(PsiElement psiElement) {
        return PsiTreeUtil.getParentOfType(psiElement, PsiMethod.class);
    }




}
