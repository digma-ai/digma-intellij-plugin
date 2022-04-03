package org.digma.intellij.plugin.psi.java;

import com.intellij.lang.Language;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import org.digma.intellij.plugin.psi.LanguageService;

public class JavaLanguageService implements LanguageService {


    @Override
    public boolean accept(Language language) {
        return JavaLanguage.INSTANCE.equals(language);
    }

    @Override
    public PsiElement getMethod(PsiElement psiElement) {
        return PsiTreeUtil.getParentOfType(psiElement, PsiMethod.class);
    }
}
