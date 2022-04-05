package org.digma.intellij.plugin.psi;

import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public interface LanguageService {

    boolean accept(Language language);

    PsiElement findParentMethodIfAny(PsiElement psiElement);

    @NotNull
    PsiElement getParentMethod(PsiElement psiElement);
}
