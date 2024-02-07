package org.digma.intellij.plugin.rider.psi.csharp;

import com.intellij.codeInsight.codeVision.CodeVisionEntry;
import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.*;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.jetbrains.rdclient.util.idea.LifetimedProjectComponent;
import com.jetbrains.rider.projectView.SolutionLifecycleHost;
import kotlin.Pair;
import org.apache.commons.lang3.time.StopWatch;
import org.digma.intellij.plugin.common.*;
import org.digma.intellij.plugin.editor.EditorUtils;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.*;
import org.digma.intellij.plugin.psi.*;
import org.digma.intellij.plugin.rider.protocol.*;
import org.digma.intellij.plugin.ui.CaretContextService;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.digma.intellij.plugin.common.CatchingUtilsKt.executeCatchingWithResult;
import static org.digma.intellij.plugin.common.PsiAccessUtilsKt.runInReadAccessWithResult;

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

    @Nullable
    @Override
    public Language getLanguageForClass(@NotNull String className) {
        //todo: implement
//        if (LanguageServiceHost.getInstance(project).isCSharpClass(className)) {
//            return CSharpLanguageUtil.getCSharpLanguageInstanceWithReflection();
//        }
        return null;
    }

    @Override
    public boolean isSupportedFile(@NotNull Project project, @NotNull VirtualFile newFile) {

        if (!VfsUtilsKt.isValidVirtualFile(newFile)) {
            return false;
        }

        PsiFile psiFile = runInReadAccessWithResult(() -> PsiManager.getInstance(project).findFile(newFile));
        if (!PsiUtils.isValidPsiFile(psiFile)) {
            return false;
        }
        return isSupportedFile(psiFile);
    }

    @Override
    public boolean isSupportedFile(PsiFile psiFile) {
        if (!PsiUtils.isValidPsiFile(psiFile)) {
            return false;
        }
        return CSharpLanguageUtil.isCSharpLanguage(psiFile.getLanguage());
    }


    //CSharpLanguageService needs the Editor to take the projectModelId which is the preferred way to find a IPsiSourceFile in resharper.
    // so try to always send the editor to this method.
    @Override
    @NotNull
    public MethodUnderCaret detectMethodUnderCaret(@NotNull Project project, @NotNull PsiFile psiFile, @Nullable Editor selectedEditor, int caretOffset) {

        //detectMethodUnderCaret should run very fast and return a result,
        // this operation may become invalid very soon if user clicks somewhere else.
        // no retry because it needs to complete very fast
        //it may be called from EDT or background, runInReadAccessWithResult will acquire read access if necessary.
        return executeCatchingWithResult(() -> PsiAccessUtilsKt.runInReadAccessWithResult(() -> LanguageServiceHost.getInstance(project).detectMethodUnderCaret(psiFile, selectedEditor, caretOffset)), throwable -> {
            ErrorReporter.getInstance().reportError(getClass().getSimpleName() + ".detectMethodUnderCaret", throwable);
            return new MethodUnderCaret("", "", "", "", PsiUtils.psiFileToUri(psiFile), caretOffset, null, false);
        });
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
                    if (VfsUtilsKt.isValidVirtualFile(file)) {
                        var psiFile = PsiManager.getInstance(project).findFile(file);
                        if (PsiUtils.isValidPsiFile(psiFile) && isRelevant(psiFile.getVirtualFile())) {
                            var selectedTextEditor = EditorUtils.getSelectedTextEditorForFile(file, FileEditorManager.getInstance(project));
                            if (selectedTextEditor != null) {
                                int offset = selectedTextEditor.getCaretModel().getOffset();
                                var methodUnderCaret = detectMethodUnderCaret(project, psiFile, selectedTextEditor, offset);
                                CaretContextService.getInstance(project).contextChanged(methodUnderCaret);
                            }
                        }
                    }
                }
            });
        }


        CodeLensHost.getInstance(project).environmentChanged(newEnv);
    }


    @Override
    public @NotNull DocumentInfo buildDocumentInfo(@NotNull PsiFile psiFile, BuildDocumentInfoProcessContext context) {
        return buildDocumentInfo(psiFile, null, context);
    }

    @Override
    public @NotNull DocumentInfo buildDocumentInfo(@NotNull PsiFile psiFile, @Nullable FileEditor newEditor, BuildDocumentInfoProcessContext context) {

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
        if (!PsiUtils.isValidPsiFile(psiFile)) {
            return false;
        }

        return isRelevant(psiFile);

    }

    @Override
    public boolean isRelevant(PsiFile psiFile) {
        return ReadActions.ensureReadAction(() ->
                PsiUtils.isValidPsiFile(psiFile) &&
                psiFile.isWritable() &&
                        isSupportedFile(psiFile));

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

    @Override
    public @Nullable PsiElement getPsiElementForMethod(@NotNull String methodId) {
        //todo: implement
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
}
