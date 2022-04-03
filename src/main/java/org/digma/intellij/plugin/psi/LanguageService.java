package org.digma.intellij.plugin.psi;

import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;

public interface LanguageService {

    boolean accept(Language language);

    PsiElement getMethod(PsiElement psiElement);
}
