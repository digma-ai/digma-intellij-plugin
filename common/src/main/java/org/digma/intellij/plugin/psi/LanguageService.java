package org.digma.intellij.plugin.psi;

import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.digma.intellij.plugin.log.Log;
import org.jetbrains.annotations.*;

public interface LanguageService {

    Logger LOGGER = Logger.getInstance(LanguageService.class);

    boolean accept(Language language);

//    MethodIdentifier findParentMethodIfAny(PsiElement psiElement);
//
//    @NotNull
//    MethodIdentifier getParentMethod(PsiElement psiElement);

    @Nullable
    MethodIdentifier detectMethodUnderCaret(Project project, PsiFile psiFile, int caretOffset);

    //for use by language services that have a psi impl, python, java, Not rider
    @Nullable
    default PsiElement findElementUnderCaret(Project project, PsiFile psiFile, int caretOffset) {
        if (psiFile != null) {
            PsiElement psiElement = psiFile.findElementAt(caretOffset);
            Log.log(LOGGER::debug, "got psi element {}", psiElement);
            return psiElement;
        }
        return null;
    }
}
