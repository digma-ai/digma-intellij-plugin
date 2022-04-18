package org.digma.intellij.plugin.psi.python;

import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.PythonLanguage;
import com.jetbrains.python.psi.PyFunction;
import org.digma.intellij.plugin.psi.*;
import org.jetbrains.annotations.Nullable;

public class PythonLanguageService implements LanguageService {

    @Override
    public boolean accept(Language language) {
        return PythonLanguage.INSTANCE.equals(language);
    }


    @Override
    @Nullable
    public MethodIdentifier detectMethodUnderCaret(Project project, PsiFile psiFile, int caretOffset) {
        PsiElement underCaret = findElementUnderCaret(project, psiFile, caretOffset);
        PyFunction psiMethod = PsiTreeUtil.getParentOfType(underCaret, PyFunction.class);
        if (psiMethod != null) {
            return new MethodIdentifier(psiMethod.getName(), psiFile.getVirtualFile().getPath());
        }
        return null;
    }

}
