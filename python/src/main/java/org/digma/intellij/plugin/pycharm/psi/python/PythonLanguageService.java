package org.digma.intellij.plugin.pycharm.psi.python;

import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.PythonLanguage;
import com.jetbrains.python.psi.PyFunction;
import kotlin.Pair;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.digma.intellij.plugin.psi.LanguageService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class PythonLanguageService implements LanguageService {


    @Override
    public Language getLanguageForMethodCodeObjectId(@NotNull String methodId) {

        //todo: implement
        return null;
    }

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
    public MethodUnderCaret detectMethodUnderCaret(@NotNull Project project, @NotNull PsiFile psiFile, int caretOffset) {
        PsiElement underCaret = findElementUnderCaret(project, psiFile, caretOffset);
        PyFunction psiMethod = PsiTreeUtil.getParentOfType(underCaret, PyFunction.class);
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
        return language.getClass().equals(PythonLanguage.class);
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

    @Override
    public boolean isIndexedLanguage() {
        return false; //todo: python is not yet indexed language, need to implement
    }

    @Override
    public DocumentInfo buildDocumentInfo(PsiFile psiFile) {
        throw new UnsupportedOperationException("should not be called, need to implement");
    }

    @Override
    public boolean isIntellijPlatformPluginLanguage() {
        return true;
    }
}
