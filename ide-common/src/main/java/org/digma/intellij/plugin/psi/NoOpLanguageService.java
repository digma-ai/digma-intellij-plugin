package org.digma.intellij.plugin.psi;

import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import kotlin.Pair;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NoOpLanguageService implements LanguageService {

    public static final NoOpLanguageService INSTANCE = new NoOpLanguageService();

    @Override
    public Language getLanguageForMethodCodeObjectId(@NotNull String methodId) {
        return new Language("NoLang") {
        };
    }

    @Override
    public boolean isSupportedFile(Project project, VirtualFile newFile) {
        return false;
    }

    @Override
    public boolean isSupportedFile(Project project, PsiFile psiFile) {
        return false;
    }

    @Override
    @NotNull
    public MethodUnderCaret detectMethodUnderCaret(@NotNull Project project, @NotNull PsiFile psiFile, int caretOffset) {
        return MethodUnderCaret.getEMPTY();
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

    @Override
    public boolean isIndexedLanguage() {
        return false;
    }

    @Override
    public DocumentInfo buildDocumentInfo(PsiFile psiFile) {
        throw new UnsupportedOperationException("should not be called");
    }

    @Override
    public boolean isIntellijPlatformPluginLanguage() {
        return false;
    }

    @Override
    public void enrichDocumentInfo(DocumentInfo documentInfo, PsiFile psiFile) {

    }
}
