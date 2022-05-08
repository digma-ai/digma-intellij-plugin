package org.digma.intellij.plugin.psi.java;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import org.digma.intellij.plugin.model.MethodUnderCaret;
import org.digma.intellij.plugin.psi.LanguageService;
import org.jetbrains.annotations.Nullable;

public class JavaLanguageService implements LanguageService {


    @Override
    public boolean isSupportedFile(Project project, VirtualFile newFile) {
        PsiFile psiFile = com.intellij.psi.PsiManager.getInstance(project).findFile(newFile);
        return JavaLanguage.INSTANCE.equals(psiFile.getLanguage());
    }

    @Override
    public boolean isSupportedFile(Project project, PsiFile psiFile) {
        return JavaLanguage.INSTANCE.equals(psiFile.getLanguage());
    }

    @Override
    @Nullable
    public MethodUnderCaret detectMethodUnderCaret(Project project, PsiFile psiFile, int caretOffset) {
        PsiElement underCaret = findElementUnderCaret(project, psiFile, caretOffset);
        PsiMethod psiMethod = PsiTreeUtil.getParentOfType(underCaret, PsiMethod.class);
        if (psiMethod != null) {
            return new MethodUnderCaret(psiMethod.getName(),psiMethod.getContainingClass().getName(), psiFile.getVirtualFile().getPath());
        }
        return null;
    }

}
