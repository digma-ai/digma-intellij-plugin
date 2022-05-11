package org.digma.intellij.plugin.pycharm.psi.python;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.PythonLanguage;
import com.jetbrains.python.psi.PyFunction;
import org.digma.intellij.plugin.model.MethodUnderCaret;
import org.digma.intellij.plugin.psi.LanguageService;
import org.jetbrains.annotations.Nullable;

public class PythonLanguageService implements LanguageService {


    @Override
    public boolean isSupportedFile(Project project, VirtualFile newFile) {
        PsiFile psiFile = com.intellij.psi.PsiManager.getInstance(project).findFile(newFile);
        return PythonLanguage.INSTANCE.equals(psiFile.getLanguage());
    }

    @Override
    public boolean isSupportedFile(Project project, PsiFile psiFile) {
        return PythonLanguage.INSTANCE.equals(psiFile.getLanguage());
    }

    @Override
    @Nullable
    public MethodUnderCaret detectMethodUnderCaret(Project project, PsiFile psiFile, int caretOffset) {
        PsiElement underCaret = findElementUnderCaret(project, psiFile, caretOffset);
        PyFunction psiMethod = PsiTreeUtil.getParentOfType(underCaret, PyFunction.class);
        if (psiMethod != null) {
            return new MethodUnderCaret(psiMethod.getName(),psiMethod.getName(), psiMethod.getContainingClass().getName(),psiFile.getVirtualFile().getPath());
        }
        return null;
    }

}
