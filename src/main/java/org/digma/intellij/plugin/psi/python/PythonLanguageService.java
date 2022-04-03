package org.digma.intellij.plugin.psi.python;

import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.PythonLanguage;
import com.jetbrains.python.psi.PyFunction;
import org.digma.intellij.plugin.psi.LanguageService;

public class PythonLanguageService implements LanguageService {

    @Override
    public boolean accept(Language language) {
        return PythonLanguage.INSTANCE.equals(language);
    }

    @Override
    public PsiElement getMethod(PsiElement psiElement) {
        return PsiTreeUtil.getParentOfType(psiElement, PyFunction.class);
    }
}
