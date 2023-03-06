package org.digma.intellij.plugin.rider.psi.csharp;

import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.jetbrains.rider.ideaInterop.fileTypes.csharp.CSharpLanguage;
import kotlin.Pair;
import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.lang3.time.StopWatch;
import org.digma.intellij.plugin.common.EDT;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.psi.PsiUtils;
import org.digma.intellij.plugin.rider.protocol.CodeLensHost;
import org.digma.intellij.plugin.rider.protocol.LanguageServiceHost;
import org.digma.intellij.plugin.ui.CaretContextService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CSharpLanguageService implements LanguageService {

    private final Logger LOGGER = Logger.getInstance(CSharpLanguageService.class);

    private final Project project;

    private final LRUMap<String, Boolean> csharpMethodCache = new LRUMap<>();

    private final CaretContextService caretContextService;


    public CSharpLanguageService(Project project) {
        caretContextService = project.getService(CaretContextService.class);
        this.project = project;
    }

    @Override
    public void ensureStartup(@NotNull Project project) {
        //make sure LanguageServiceHost is initialized on EDT
        LanguageServiceHost.getInstance(project);
        CodeLensHost.getInstance(project);
    }

    @Nullable
    @Override
    public Language getLanguageForMethodCodeObjectId(@NotNull String methodId) {
        //calls to this method with the same argument may happen many times.
        // but languageServiceHost.isCSharpMethod is a call to resharper which is not the best performance,
        // so keep all methods ids in a simple cache for later use.

        if (csharpMethodCache.containsKey(methodId)) {
            return Boolean.TRUE.equals(csharpMethodCache.get(methodId)) ? CSharpLanguage.INSTANCE : null;
        }

        if (LanguageServiceHost.getInstance(project).isCSharpMethod(methodId)) {
            csharpMethodCache.put(methodId, Boolean.TRUE);
            return CSharpLanguage.INSTANCE;
        }
        csharpMethodCache.put(methodId, Boolean.FALSE);
        return null;
    }

    @Override
    public boolean isSupportedFile(@NotNull Project project, @NotNull VirtualFile newFile) {
        PsiFile psiFile = com.intellij.psi.PsiManager.getInstance(project).findFile(newFile);
        if (psiFile == null) {
            return false;
        }
        return CSharpLanguage.INSTANCE.equals(psiFile.getLanguage());
    }

    @Override
    public boolean isSupportedFile(Project project, PsiFile psiFile) {
        return CSharpLanguage.INSTANCE.equals(psiFile.getLanguage());
    }


    //CSharpLanguageService needs the Editor to tale the projectModelId which is the preferred way to find a IPsiSourceFile in reshrper.
    @Override
    @NotNull
    public MethodUnderCaret detectMethodUnderCaret(@NotNull Project project, @NotNull PsiFile psiFile, @Nullable Editor selectedEditor, int caretOffset) {
        return LanguageServiceHost.getInstance(project).detectMethodUnderCaret(psiFile,selectedEditor,caretOffset);
    }

    @Override
    public void navigateToMethod(String codeObjectId) {
        Log.log(LOGGER::debug, "Navigating to method {}", codeObjectId);
        LanguageServiceHost.getInstance(project).navigateToMethod(codeObjectId);
    }

    @Override
    public boolean isServiceFor(@NotNull Language language) {
        return language.getClass().equals(CSharpLanguage.class);
    }


    @Override
    public Map<String, String> findWorkspaceUrisForCodeObjectIds(List<String> codeObjectIds) {
        return LanguageServiceHost.getInstance(project).findWorkspaceUrisForCodeObjectIds(codeObjectIds);
    }

    @Override
    public Map<String, Pair<String, Integer>> findWorkspaceUrisForSpanIds(List<String> spanIds) {
        var stopWatch = StopWatch.createStarted();
        try {
            return LanguageServiceHost.getInstance(project).findWorkspaceUrisForSpanIds(spanIds);
        } finally {
            stopWatch.stop();
            Log.log(LOGGER::debug, "findWorkspaceUrisForSpanIds time took {} milliseconds", stopWatch.getTime(TimeUnit.MILLISECONDS));
        }
    }

    @Override
    public void environmentChanged(String newEnv) {

        EDT.ensureEDT(() -> {
            var fileEditor = FileEditorManager.getInstance(project).getSelectedEditor();
            if (fileEditor != null) {
                var file = fileEditor.getFile();
                var psiFile = PsiManager.getInstance(project).findFile(file);
                if (psiFile != null && isRelevant(psiFile.getVirtualFile())) {
                    var selectedTextEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
                    if (selectedTextEditor != null) {
                        int offset = selectedTextEditor.getCaretModel().getOffset();
                        var methodUnderCaret = detectMethodUnderCaret(project, psiFile, null, offset);
                        caretContextService.contextChanged(methodUnderCaret);
                    }
                }
            }
        });


        CodeLensHost.getInstance(project).environmentChanged(newEnv);
    }


    @Override
    public @NotNull DocumentInfo buildDocumentInfo(@NotNull PsiFile psiFile) {
        return buildDocumentInfo(psiFile,null);
    }

    @Override
    public @NotNull DocumentInfo buildDocumentInfo(@NotNull PsiFile psiFile, @Nullable FileEditor newEditor) {
        DocumentInfo documentInfo =  LanguageServiceHost.getInstance(project).getDocumentInfo(psiFile,newEditor);
        if (documentInfo == null) {
            Log.log(LOGGER::warn, "DocumentInfo not found for {}, returning empty DocumentInfo", psiFile);
            documentInfo = new DocumentInfo(PsiUtils.psiFileToUri(psiFile),new HashMap<>());
        }
        return documentInfo;
    }

    @Override
    public boolean isIntellijPlatformPluginLanguage() {
        return false;
    }


    @Override
    public boolean isRelevant(VirtualFile file) {
        if (file.isDirectory()){
            return false;
        }

        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null) {
            return false;
        }

        return isRelevant(psiFile);

    }

    @Override
    public boolean isRelevant(PsiFile psiFile) {
        return psiFile.isValid() &&
                psiFile.isWritable() &&
                isSupportedFile(project, psiFile);
    }

    @Override
    public void refreshMethodUnderCaret(@NotNull Project project, @NotNull PsiFile psiFile, @Nullable Editor selectedEditor, int offset) {
        MethodUnderCaret methodUnderCaret = detectMethodUnderCaret(project, psiFile, selectedEditor, offset);
        caretContextService.contextChanged(methodUnderCaret);
    }
}
