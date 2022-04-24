package org.digma.intellij.plugin.psi.python;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.PythonLanguage;
import com.jetbrains.python.psi.PyFunction;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.psi.MethodIdentifier;
import org.jetbrains.annotations.Nullable;

public class PythonLanguageService implements LanguageService {


    @Override
    public boolean isSupportedFile(Project project, VirtualFile newFile) {
        PsiFile psiFile = com.intellij.psi.PsiManager.getInstance(project).findFile(newFile);
        return PythonLanguage.INSTANCE.equals(psiFile.getLanguage());
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
