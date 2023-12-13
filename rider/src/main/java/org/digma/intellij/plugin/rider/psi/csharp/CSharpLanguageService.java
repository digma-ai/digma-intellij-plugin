package org.digma.intellij.plugin.rider.psi.csharp;

import com.intellij.codeInsight.codeVision.CodeVisionEntry;
import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.jetbrains.rdclient.util.idea.LifetimedProjectComponent;
import com.jetbrains.rider.projectView.SolutionLifecycleHost;
import kotlin.Pair;
import org.apache.commons.lang3.time.StopWatch;
import org.digma.intellij.plugin.common.EDT;
import org.digma.intellij.plugin.editor.EditorUtils;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.EndpointInfo;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.psi.PsiUtils;
import org.digma.intellij.plugin.rider.protocol.CodeLensHost;
import org.digma.intellij.plugin.rider.protocol.LanguageServiceHost;
import org.digma.intellij.plugin.ui.CaretContextService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class CSharpLanguageService extends LifetimedProjectComponent implements LanguageService {

    private static final Logger LOGGER = Logger.getInstance(CSharpLanguageService.class);

    private final Project project;


    /*
    It's better, as much as possible, in language services especially, not to initialize service dependencies in the constructor but use
    a getInstance for services when they are first needed. that will minimize the possibility for cyclic dependencies.
     */
    public CSharpLanguageService(Project project) {
        super(project);
        this.project = project;
    }

    @Override
    public void ensureStartupOnEDT(@NotNull Project project) {
        Log.log(LOGGER::debug, "ensureStartupOnEDT called, backend loaded: {}", SolutionLifecycleHost.Companion.getInstance(project).isBackendLoaded().getValue());
        //make sure LanguageServiceHost is initialized on EDT, for example project.solution.languageServiceModel must be
        // called on EDT
        LanguageServiceHost.getInstance(project);
        CodeLensHost.getInstance(project);
    }

    @Override
    public void runWhenSmart(Runnable task) {

        Runnable r = () -> {
            if (DumbService.isDumb(project)) {
                DumbService.getInstance(project).runWhenSmart(task);
            } else {
                task.run();
            }
        };

        LanguageServiceHost.getInstance(project).runIfSolutionLoaded(r);

    }

    @Nullable
    @Override
    public Language getLanguageForMethodCodeObjectId(@NotNull String methodId) {

        if (methodId.indexOf("$_$") <= 0) {
            Log.log(LOGGER::debug, "method id in getLanguageForMethodCodeObjectId does not contain $_$ {}", methodId);
            return null;
        }

        if (LanguageServiceHost.getInstance(project).isCSharpMethod(methodId)) {
            return CSharpLanguageUtil.getCSharpLanguageInstanceWithReflection();
        }
        return null;
    }

    @Override
    public boolean isSupportedFile(@NotNull Project project, @NotNull VirtualFile newFile) {
        PsiFile psiFile = com.intellij.psi.PsiManager.getInstance(project).findFile(newFile);
        if (psiFile == null) {
            return false;
        }
        return CSharpLanguageUtil.isCSharpLanguage(psiFile.getLanguage());
    }

    @Override
    public boolean isSupportedFile(Project project, PsiFile psiFile) {
        return CSharpLanguageUtil.isCSharpLanguage(psiFile.getLanguage());
    }


    //CSharpLanguageService needs the Editor to take the projectModelId which is the preferred way to find a IPsiSourceFile in resharper.
    // so try to always send the editor to this method.
    @Override
    @NotNull
    public MethodUnderCaret detectMethodUnderCaret(@NotNull Project project, @NotNull PsiFile psiFile, @Nullable Editor selectedEditor, int caretOffset) {
        return LanguageServiceHost.getInstance(project).detectMethodUnderCaret(psiFile, selectedEditor, caretOffset);
    }

    @Override
    public @Nullable String detectMethodBySpan(@NotNull Project project, String spanCodeObjectId) {
        return null;
    }

    @Override
    public void navigateToMethod(String methodId) {

        Log.log(LOGGER::debug, "got navigate to method request {}", methodId);
        if (methodId.indexOf("$_$") <= 0) {
            Log.log(LOGGER::debug, "method id in navigateToMethod does not contain $_$, can not navigate {}", methodId);
            return;
        }

        LanguageServiceHost.getInstance(project).navigateToMethod(methodId);
    }

    @Override
    public boolean isServiceFor(@NotNull Language language) {
        return CSharpLanguageUtil.isCSharpLanguage(language);
    }


    @Override
    public Map<String, String> findWorkspaceUrisForCodeObjectIdsForErrorStackTrace(List<String> codeObjectIds) {
        return ProgressManager.getInstance().
                computeInNonCancelableSection(() -> LanguageServiceHost.getInstance(project).findWorkspaceUrisForCodeObjectIdsForErrorStackTrace(codeObjectIds));
    }

    @Override
    public Map<String, Pair<String, Integer>> findWorkspaceUrisForMethodCodeObjectIds(List<String> methodCodeObjectIds) {
        return ProgressManager.getInstance().
                computeInNonCancelableSection(() -> LanguageServiceHost.getInstance(project).findWorkspaceUrisForMethodCodeObjectIds(methodCodeObjectIds));
    }

    @Override
    public Map<String, Pair<String, Integer>> findWorkspaceUrisForSpanIds(List<String> spanIds) {
        var stopWatch = StopWatch.createStarted();
        try {
            return ProgressManager.getInstance().
                    computeInNonCancelableSection(() -> LanguageServiceHost.getInstance(project).findWorkspaceUrisForSpanIds(spanIds));

        } finally {
            stopWatch.stop();
            Log.log(LOGGER::debug, "findWorkspaceUrisForSpanIds time took {} milliseconds", stopWatch.getTime(TimeUnit.MILLISECONDS));
        }
    }

    @Override
    public Set<EndpointInfo> lookForDiscoveredEndpoints(String endpointId) {
        return Collections.emptySet();
    }

    @Override
    public void environmentChanged(String newEnv, boolean refreshInsightsView) {

        if (refreshInsightsView) {
            EDT.ensureEDT(() -> {
                var fileEditor = FileEditorManager.getInstance(project).getSelectedEditor();
                if (fileEditor != null) {
                    var file = fileEditor.getFile();
                    var psiFile = PsiManager.getInstance(project).findFile(file);
                    if (psiFile != null && isRelevant(psiFile.getVirtualFile())) {
                        var selectedTextEditor = EditorUtils.getSelectedTextEditorForFile(file, FileEditorManager.getInstance(project));
                        if (selectedTextEditor != null) {
                            int offset = selectedTextEditor.getCaretModel().getOffset();
                            var methodUnderCaret = detectMethodUnderCaret(project, psiFile, selectedTextEditor, offset);
                            CaretContextService.getInstance(project).contextChanged(methodUnderCaret);
                        }
                    }
                }
            });
        }


        CodeLensHost.getInstance(project).environmentChanged(newEnv);
    }


    @Override
    public @NotNull DocumentInfo buildDocumentInfo(@NotNull PsiFile psiFile) {
        return buildDocumentInfo(psiFile, null);
    }

    @Override
    public @NotNull DocumentInfo buildDocumentInfo(@NotNull PsiFile psiFile, @Nullable FileEditor newEditor) {

        Log.log(LOGGER::debug, "got buildDocumentInfo request for {}", psiFile);
        //must be PsiJavaFile , this method should be called only for java files
        if (CSharpLanguageUtil.isCSharpFile(psiFile)) {
            DocumentInfo documentInfo = LanguageServiceHost.getInstance(project).getDocumentInfo(psiFile, newEditor);
            if (documentInfo == null) {
                Log.log(LOGGER::warn, "DocumentInfo not found for {}, returning empty DocumentInfo", psiFile);
                documentInfo = new DocumentInfo(PsiUtils.psiFileToUri(psiFile), new HashMap<>());
            }
            return documentInfo;
        } else {
            Log.log(LOGGER::debug, "psi file is noy CSharpFile, returning empty DocumentInfo for {}", psiFile);
            return new DocumentInfo(PsiUtils.psiFileToUri(psiFile), new HashMap<>());
        }
    }


    @Override
    public boolean isRelevant(VirtualFile file) {
        if (file.isDirectory()) {
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
        CaretContextService.getInstance(project).contextChanged(methodUnderCaret);
    }

    @Override
    public boolean isCodeVisionSupported() {
        return false;
    }

    @Override
    public @NotNull List<Pair<TextRange, CodeVisionEntry>> getCodeLens(@NotNull PsiFile psiFile) {
        throw new UnsupportedOperationException("should not be called for CSharpLanguageService");
    }
}
