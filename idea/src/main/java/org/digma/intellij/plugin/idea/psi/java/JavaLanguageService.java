package org.digma.intellij.plugin.idea.psi.java;

import com.intellij.lang.Language;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import kotlin.Pair;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.digma.intellij.plugin.psi.LanguageService;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

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
            return new MethodUnderCaret(psiMethod.getName(), psiMethod.getName(), psiMethod.getContainingClass().getName(), psiFile.getVirtualFile().getPath());
        }
        return null;
    }

    @Override
    public void navigateToMethod(String codeObjectId) {

    }

    @Override
    public boolean isServiceFor(Language language) {
        return language.getClass().equals(JavaLanguage.class);
    }

    @Override
    public Map<String, String> findWorkspaceUrisForCodeObjectIds(List<String> codeObjectIds) {
        return null;
    }

    @Override
    public Map<String, Pair<String, Integer>> findWorkspaceUrisForSpanIds(List<String> spanIds) {
        return null;
    }

    @Override
    public void environmentChanged(String newEnv) {

    }

}
