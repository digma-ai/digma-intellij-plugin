package org.digma.intellij.plugin.psi;

import com.intellij.lang.Language;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import kotlin.Pair;
import org.digma.intellij.plugin.document.BuildDocumentInfoProcessContext;
import org.digma.intellij.plugin.instrumentation.*;
import org.digma.intellij.plugin.model.discovery.*;
import org.jetbrains.annotations.*;

import java.util.*;

public abstract class OldNoOpLanguageService implements LanguageService {

    public static final NoOpLanguageService INSTANCE = new NoOpLanguageService();


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
    public Language getLanguageForClass(@NotNull String methodId) {
        return new Language("NoLang") {
        };
    }

    @Override
    public boolean isSupportedFile(Project project, VirtualFile newFile) {
        return false;
    }

    @Override
    public boolean isSupportedFile(PsiFile psiFile) {
        return false;
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


    //this method should never be called on NoOpLanguageService, calling code must be aware of that
//    @Override
    public @NotNull DocumentInfo buildDocumentInfo(@NotNull PsiFileCachedValueWithUri psiFileCachedValue, BuildDocumentInfoProcessContext context) {
        throw new UnsupportedOperationException("should not be called");
    }

    //this method should never be called on NoOpLanguageService, calling code must be aware of that
//    @Override
    public @NotNull DocumentInfo buildDocumentInfo(@NotNull PsiFileCachedValueWithUri psiFileCachedValue, @Nullable FileEditor newEditor, BuildDocumentInfoProcessContext context) {
        throw new UnsupportedOperationException("should not be called");
    }


    @Override
    public @Nullable PsiElement getPsiElementForMethod(@NotNull String methodId) {
        return null;
    }

    @Override
    public @Nullable PsiElement getPsiElementForClassByMethodId(@NotNull String methodId) {
        return null;
    }

    @Override
    public @Nullable PsiElement getPsiElementForClassByName(@NotNull String className) {
        return null;
    }

    @Override
    public @NotNull InstrumentationProvider getInstrumentationProvider() {
        return new NoOpInstrumentationProvider();
    }

    @Override
    public @NotNull Map<String, PsiElement> findMethodsByCodeObjectIds(@NotNull PsiFile psiFile, @NotNull List<String> methodIds) {
        return Map.of();
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
    public boolean isCodeVisionSupported() {
        return false;
    }

}
