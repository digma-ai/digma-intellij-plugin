package org.digma.intellij.plugin.psi;

import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import kotlin.Pair;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NoOpLanguageService implements LanguageService {

    public static final NoOpLanguageService INSTANCE = new NoOpLanguageService();

    @Override
    public boolean isSupportedFile(Project project, VirtualFile newFile) {
        return false;
    }

    @Override
    public boolean isSupportedFile(Project project, PsiFile psiFile) {
        return false;
    }

    @Override
    public @Nullable MethodUnderCaret detectMethodUnderCaret(Project project, PsiFile psiFile, int caretOffset) {
        return null;
    }

    @Override
    public void navigateToMethod(String codeObjectId) {

    }

    @Override
    public boolean isServiceFor(Language language) {
        return false;
    }

    @Override
    public Map<String, String> findWorkspaceUrisForCodeObjectIds(List<String> codeObjectIds) {
        return new HashMap<>();
    }

    @Override
    public Map<String, Pair<String, Integer>> findWorkspaceUrisForSpanIds(List<String> spanIds) {
        return new HashMap<>();
    }

    @Override
    public void environmentChanged(String newEnv) {

    }
}
