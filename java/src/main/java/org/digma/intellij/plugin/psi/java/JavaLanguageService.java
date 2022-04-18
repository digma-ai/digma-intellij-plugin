package org.digma.intellij.plugin.psi.java;

import com.intellij.lang.Language;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.digma.intellij.plugin.psi.*;
import org.jetbrains.annotations.Nullable;

public class JavaLanguageService implements LanguageService {

    @Override
    public boolean accept(Language language) {
        return JavaLanguage.INSTANCE.equals(language);
    }

    @Override
    @Nullable
    public MethodIdentifier detectMethodUnderCaret(Project project, PsiFile psiFile, int caretOffset) {
        PsiElement underCaret = findElementUnderCaret(project, psiFile, caretOffset);
        PsiMethod psiMethod = PsiTreeUtil.getParentOfType(underCaret, PsiMethod.class);
        if (psiMethod != null) {
            return new MethodIdentifier(psiMethod.getName(), psiFile.getVirtualFile().getPath());
        }
        return null;
    }

}
