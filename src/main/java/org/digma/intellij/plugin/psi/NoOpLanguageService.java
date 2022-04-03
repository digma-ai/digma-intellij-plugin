package org.digma.intellij.plugin.psi;

import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;

class NoOpLanguageService implements LanguageService {

    @Override
    public boolean accept(Language language) {
        return false;
    }

    @Override
    public PsiElement getMethod(PsiElement psiElement) {
        return null;
    }
}
