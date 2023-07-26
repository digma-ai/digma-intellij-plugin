package org.digma.intellij.plugin.psi;

import com.intellij.codeInsight.codeVision.CodeVisionEntry;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import kotlin.Pair;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.EndpointInfo;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NoOpLanguageService implements LanguageService {

    public static final NoOpLanguageService INSTANCE = new NoOpLanguageService();

    @Override
    public void ensureStartupOnEDT(@NotNull Project project) {
        //nothing to do
    }

    @Override
    public void runWhenSmart(Runnable task) {
        //nothing to do
    }

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
    public MethodUnderCaret detectMethodUnderCaret(@NotNull Project project, @NotNull PsiFile psiFile, Editor selectedEditor, int caretOffset) {
        return MethodUnderCaret.getEMPTY();
    }

    @Override
    public void navigateToMethod(String methodId) {
        //nothing to do
    }

    @Override
    public boolean isServiceFor(@NotNull Language language) {
        return false;
    }

    @Override
    public Map<String, String> findWorkspaceUrisForCodeObjectIdsForErrorStackTrace(List<String> codeObjectIds) {
        return new HashMap<>();
    }

    @Override
    public Map<String, Pair<String, Integer>> findWorkspaceUrisForMethodCodeObjectIds(List<String> methodCodeObjectIds) {
        return new HashMap<>();
    }

    @Override
    public Map<String, Pair<String, Integer>> findWorkspaceUrisForSpanIds(List<String> spanIds) {
        return new HashMap<>();
    }

    @Override
    public Set<EndpointInfo> lookForDiscoveredEndpoints(String endpointId) {
        return Collections.emptySet();
    }

    @Override
    public void environmentChanged(String newEnv, boolean refreshInsightsView) {
        //nothing to do
    }


    //this method should never be called on NoOpLanguageService, calling code must be aware of that
    @Override
    public @NotNull DocumentInfo buildDocumentInfo(@NotNull PsiFile psiFile) {
        throw new UnsupportedOperationException("should not be called");
    }

    //this method should never be called on NoOpLanguageService, calling code must be aware of that
    @Override
    public @NotNull DocumentInfo buildDocumentInfo(@NotNull PsiFile psiFile, @Nullable FileEditor newEditor) {
        throw new UnsupportedOperationException("should not be called");
    }


    @Override
    public boolean isRelevant(VirtualFile file) {
        return false;
    }

    @Override
    public boolean isRelevant(PsiFile psiFile) {
        return false;
    }

    @Override
    public void refreshMethodUnderCaret(@NotNull Project project, @NotNull PsiFile psiFile, @Nullable Editor selectedEditor, int offset) {
        //nothing to do
    }

    @Override
    public boolean isCodeVisionSupported() {
        return false;
    }

    @Override
    public @NotNull List<Pair<TextRange, CodeVisionEntry>> getCodeLens(@NotNull PsiFile psiFile) {
        throw new UnsupportedOperationException("should not be called for NoOPLanguageService");
    }
}
