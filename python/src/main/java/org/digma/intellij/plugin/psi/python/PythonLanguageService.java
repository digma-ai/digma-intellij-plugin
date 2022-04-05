package org.digma.intellij.plugin.psi.python;

import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.PythonLanguage;
import com.jetbrains.python.psi.PyFunction;
import org.digma.intellij.plugin.psi.LanguageService;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PythonLanguageService implements LanguageService {

    @Override
    public boolean accept(Language language) {
        return PythonLanguage.INSTANCE.equals(language);
    }

    @Override
    public PsiElement findParentMethodIfAny(PsiElement psiElement) {
        return PsiTreeUtil.getParentOfType(psiElement, PyFunction.class);
    }

    @Override
    @NotNull
    public PsiElement getParentMethod(PsiElement psiElement) {
        return Objects.requireNonNull(PsiTreeUtil.getParentOfType(psiElement, PyFunction.class), "getParentMethod must find a parent method or we have a bug!");
    }
}
