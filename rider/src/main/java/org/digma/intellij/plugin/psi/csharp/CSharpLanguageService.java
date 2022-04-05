package org.digma.intellij.plugin.psi.csharp;

import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;
import com.jetbrains.rider.ideaInterop.fileTypes.csharp.CSharpLanguage;
import org.digma.intellij.plugin.psi.LanguageService;
import org.jetbrains.annotations.NotNull;

public class CSharpLanguageService implements LanguageService {
    @Override
    public boolean accept(Language language) {
        return CSharpLanguage.INSTANCE.equals(language);
    }

    @Override
    public PsiElement findParentMethodIfAny(PsiElement psiElement) {
        //todo: temp impl. rider does not have full PSI support, need resharper
        while (psiElement.getParent() != null){
            if (psiElement.getText().contains("private ") || psiElement.getText().contains("public ")){
                return psiElement;
            }
            psiElement = psiElement.getParent();
        }

        return psiElement;
    }

    @Override
    public @NotNull PsiElement getParentMethod(PsiElement psiElement) {
        //todo: temp impl. rider does not have full PSI support, need resharper
        return findParentMethodIfAny(psiElement);
    }
}
