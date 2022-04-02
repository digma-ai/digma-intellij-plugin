package org.digma.intellij.plugin.psi;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.psi.PyFunction;
import org.digma.intellij.plugin.log.Log;

/**
 * Query and navigate PSI elements.
 * This is the only class dealing with language specific elements.
 */
public class PsiNavigator {

    private static final Logger LOGGER = Logger.getInstance(PsiNavigator.class);

    //todo: handle all possible languages

    public static boolean isMethod(PsiElement psiElement) {
        return maybeGetMethod(psiElement) != null;
    }


    //todo: can return null
    public static PsiElement maybeGetMethod(PsiElement psiElement) {
        return getMethod(psiElement);
    }


    //todo: can not return null
    public static PsiElement getMethod(PsiElement psiElement) {
        Log.log(LOGGER::debug, "in getMethod, got element {}", psiElement);
        PsiElement method = PsiTreeUtil.getParentOfType(psiElement, PyFunction.class);
        Log.log(LOGGER::debug, "in getMethod, got method? {}", method);
        return method;
    }


}
