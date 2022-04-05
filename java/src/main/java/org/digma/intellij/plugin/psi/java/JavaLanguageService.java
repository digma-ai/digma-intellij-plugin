package org.digma.intellij.plugin.psi.java;

import com.intellij.lang.Language;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.digma.intellij.plugin.psi.LanguageService;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class JavaLanguageService implements LanguageService {


    @Override
    public boolean accept(Language language) {
        return JavaLanguage.INSTANCE.equals(language);
    }

    @Override
    public PsiElement findParentMethodIfAny(PsiElement psiElement) {
        return PsiTreeUtil.getParentOfType(psiElement, PsiMethod.class);
    }

    @Override
    @NotNull
    public PsiElement getParentMethod(PsiElement psiElement) {
        return Objects.requireNonNull(PsiTreeUtil.getParentOfType(psiElement, PsiMethod.class), "getParentMethod must find a parent method or we have a bug!");
    }
}
