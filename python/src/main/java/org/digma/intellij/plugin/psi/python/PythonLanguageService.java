package org.digma.intellij.plugin.psi.python;

import com.intellij.codeInsight.codeVision.CodeVisionEntry;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.*;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.PythonLanguage;
import com.jetbrains.python.psi.*;
import com.jetbrains.python.psi.stubs.*;
import kotlin.Pair;
import org.digma.intellij.plugin.common.*;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.editor.*;
import org.digma.intellij.plugin.env.Env;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.instrumentation.*;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.*;
import org.digma.intellij.plugin.psi.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static org.digma.intellij.plugin.common.CatchingUtilsKt.executeCatchingWithResult;

public class PythonLanguageService implements LanguageService {

    private static final Logger LOGGER = Logger.getInstance(PythonLanguageService.class);

    private final Project project;

    private final ProjectFileIndex projectFileIndex;

    /*
    It's better, as much as possible, in language services especially, not to initialize service dependencies in the constructor but use
    a getInstance for services when they are first needed. that will minimize the possibility for cyclic dependencies.
     */
    public PythonLanguageService(Project project) {
        this.project = project;
        projectFileIndex = project.getService(ProjectFileIndex.class);
    }


    @Override
    public void ensureStartupOnEDT(@NotNull Project project) {
        //nothing to do
    }

    @Override
    public void runWhenSmart(Runnable task) {
        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).runWhenSmart(task);
        } else {
            task.run();
        }
    }

    @Nullable
    @Override
    public Language getLanguageForMethodCodeObjectId(@NotNull String methodId) {

        var functionName = PythonLanguageUtils.extractFunctionNameFromCodeObjectId(methodId);

        //try to find a function that produces the same code object id,
        // if found return its language, else null
        return ReadAction.compute(() -> {
            var functions = PyFunctionNameIndex.find(functionName, project, GlobalSearchScope.projectScope(project));

            for (PyFunction function : functions) {
                var codeObjectId = PythonLanguageUtils.createPythonMethodCodeObjectId(project, function);
                //python method has multiple ids, we don't know what the backend will send so check with all possible ids
                List<String> allIds = PythonAdditionalIdsProvider.getAdditionalIdsInclusive(codeObjectId, false);
                if (allIds.contains(methodId)) {
                    return function.getLanguage();
                }
            }

            return null;
        });
    }


    @Override
    public @Nullable Language getLanguageForClass(@NotNull String className) {
        return ReadAction.compute(() -> {
            var classes = PyClassNameIndex.find(className, project, GlobalSearchScope.projectScope(project));
            var cls = classes.stream().findFirst();
            return cls.map(PsiElement::getLanguage).orElse(null);
        });
    }

    @Override
    public boolean isSupportedFile(@NotNull Project project, @NotNull VirtualFile newFile) {
        if (!VfsUtilsKt.isValidVirtualFile(newFile)) {
            return false;
        }
        PsiFile psiFile = PsiAccessUtilsKt.runInReadAccessWithResult(() -> PsiManager.getInstance(project).findFile(newFile));
        return PsiUtils.isValidPsiFile(psiFile) && isSupportedFile(psiFile);
    }

    @Override
    public boolean isSupportedFile(@NotNull PsiFile psiFile) {
        return PythonLanguage.INSTANCE.equals(psiFile.getLanguage());
    }

    @Override
    @NotNull
    public MethodUnderCaret detectMethodUnderCaret(@NotNull Project project, @NotNull PsiFile psiFile, Editor selectedEditor, int caretOffset) {

        //detectMethodUnderCaret should run very fast and return a result,
        // this operation may become invalid very soon if user clicks somewhere else.
        // no retry because it needs to complete very fast
        //it may be called from EDT or background, runInReadAccessWithResult will acquire read access if necessary.
        return executeCatchingWithResult(() -> PsiAccessUtilsKt.runInReadAccessWithResult(() -> detectMethodUnderCaret(project, psiFile, caretOffset)), throwable -> {
            ErrorReporter.getInstance().reportError(project, getClass().getSimpleName() + ".detectMethodUnderCaret", throwable);
            return new MethodUnderCaret("", "", "", "", PsiUtils.psiFileToUri(psiFile), caretOffset, null, false);
        });
    }

    private MethodUnderCaret detectMethodUnderCaret(@NotNull Project project, @NotNull PsiFile psiFile, int caretOffset) {

        if (!isSupportedFile(psiFile)) {
            return new MethodUnderCaret("", "", "", "", PsiUtils.psiFileToUri(psiFile), caretOffset, null, false);
        }
        PsiElement underCaret = psiFile.findElementAt(caretOffset);
        if (underCaret == null) {
            return new MethodUnderCaret("", "", "", "", PsiUtils.psiFileToUri(psiFile), caretOffset, null, true);
        }
        PyFunction pyFunction = PsiTreeUtil.getParentOfType(underCaret, PyFunction.class);
        if (pyFunction != null) {
            var methodId = PythonLanguageUtils.createPythonMethodCodeObjectId(project, pyFunction);
            var name = pyFunction.getName() == null ? "" : pyFunction.getName();
            var containingClass = pyFunction.getContainingClass();
            var className = containingClass == null ? "" : containingClass.getName() + ".";
            return new MethodUnderCaret(methodId, name, className, "", PsiUtils.psiFileToUri(psiFile), caretOffset);
        }
        return new MethodUnderCaret("", "", "", "", PsiUtils.psiFileToUri(psiFile), caretOffset);
    }

    /**
     * navigate to a method. this method is meant to be used only to navigate to a method in the current selected editor.
     * it is used from the methods preview list. it will not navigate to any method in the project.
     */
    @Override
    public void navigateToMethod(String methodId) {

        Log.log(LOGGER::debug, "got navigate to method request {}", methodId);

        var functionName = PythonLanguageUtils.extractFunctionNameFromCodeObjectId(methodId);


        ReadActions.ensureReadAction(() -> {
            var functions = PyFunctionNameIndex.find(functionName, project, GlobalSearchScope.allScope(project));

            //PyFunctionNameIndex may find many functions, we want only one, so if we found it break the loop
            boolean found = false;
            for (PyFunction function : functions) {
                if (function.isValid()) {
                    var codeObjectId = PythonLanguageUtils.createPythonMethodCodeObjectId(project, function);
                    List<String> allIds = PythonAdditionalIdsProvider.getAdditionalIdsInclusive(codeObjectId, false);
                    if (allIds.contains(methodId) && (function.canNavigateToSource())) {
                        Log.log(LOGGER::debug, "navigating to method {}", function);
                        found = true;
                        function.navigate(true);
                        break;
                    }
                }
            }

            if (!found) {
                navigateToMethodFallback(methodId);
            }
        });

    }

    private void navigateToMethodFallback(String methodId) {

        PsiFile psiFile = DocumentInfoService.getInstance(project).findPsiFileByMethodId(methodId);
        if (psiFile instanceof PyFile pyFile) {

            PyFunction pyFunction = PythonLanguageUtils.findMethodInFile(project, pyFile, methodId);

            if (pyFunction != null && pyFunction.canNavigateToSource()) {
                Log.log(LOGGER::debug, "navigating to method {}", pyFunction);
                pyFunction.navigate(true);
            } else if (pyFunction != null) {
                //it's a fallback. sometimes the psiMethod.canNavigateToSource is false and really the
                //navigation doesn't work. i can't say why. usually it happens when indexing is not ready yet,
                // and the user opens files, selects tabs or moves the caret. then when indexing is finished
                // we have the list of methods but then psiMethod.navigate doesn't work.
                // navigation to source using the editor does work in these circumstances.
                var selectedEditor = EditorUtils.getSelectedTextEditorForFile(psiFile.getVirtualFile(), FileEditorManager.getInstance(project));
                if (selectedEditor != null) {
                    Log.log(LOGGER::debug, "moving caret to offset of function {}", pyFunction);
                    selectedEditor.getCaretModel().moveToOffset(pyFunction.getTextOffset());
                } else {
                    Log.log(LOGGER::debug, "could not find selected text editor, can't navigate to method  {}", methodId);
                }
            } else {
                Log.log(LOGGER::debug, "could not navigate to method {}, can't find PsiMethod in file {}", methodId, psiFile.getVirtualFile());
            }
        }
    }


    @Override
    public boolean isServiceFor(@NotNull Language language) {
        return PythonLanguage.class.equals(language.getClass());
    }

    @Override
    public Map<String, String> findWorkspaceUrisForCodeObjectIdsForErrorStackTrace(List<String> codeObjectIds) {

        var workspaceUris = new HashMap<String, String>();

        codeObjectIds.forEach(methodId -> {

            var functionName = PythonLanguageUtils.extractFunctionNameFromCodeObjectId(methodId);

            ReadActions.ensureReadAction(() -> {
                var functions = PyFunctionNameIndex.find(functionName, project, GlobalSearchScope.projectScope(project));

                //PyFunctionNameIndex may find many functions, we want only one, so if we found it break the loop
                // assuming that our createPythonMethodCodeObjectId returns a unique id
                for (PyFunction function : functions) {
                    if (function.isValid()) {
                        PsiFile psiFile = function.getContainingFile();
                        if (PythonLanguageUtils.isProjectFile(project, psiFile)) {
                            var codeObjectId = PythonLanguageUtils.createPythonMethodCodeObjectId(project, function);
                            List<String> allIds = PythonAdditionalIdsProvider.getAdditionalIdsInclusive(codeObjectId, false);
                            if (allIds.contains(methodId)) {
                                String url = PsiUtils.psiFileToUri(psiFile);
                                workspaceUris.put(codeObjectId, url);
                                break;
                            }
                        }
                    }
                }
            });
        });

        return workspaceUris;
    }

    @Override
    public Map<String, Pair<String, Integer>> findWorkspaceUrisForMethodCodeObjectIds(List<String> methodCodeObjectIds) {

        Map<String, Pair<String, Integer>> workspaceUris = new HashMap<>();

        methodCodeObjectIds.forEach(methodId -> {

            var functionName = PythonLanguageUtils.extractFunctionNameFromCodeObjectId(methodId);

            ReadActions.ensureReadAction(() -> {
                var functions = PyFunctionNameIndex.find(functionName, project, GlobalSearchScope.projectScope(project));

                //PyFunctionNameIndex may find many functions, we want only one, so if we found it break the loop.
                // assuming that our createPythonMethodCodeObjectId returns a unique id
                for (PyFunction function : functions) {
                    if (function.isValid()) {
                        PsiFile psiFile = function.getContainingFile();
                        if (PythonLanguageUtils.isProjectFile(project, psiFile)) {
                            var codeObjectId = PythonLanguageUtils.createPythonMethodCodeObjectId(project, function);
                            List<String> allIds = PythonAdditionalIdsProvider.getAdditionalIdsInclusive(codeObjectId, false);
                            if (allIds.contains(methodId)) {
                                String url = PsiUtils.psiFileToUri(psiFile);
                                workspaceUris.put(methodId, new Pair<>(url, function.getTextOffset()));
                                break;
                            }
                        }
                    }
                }
            });
        });

        return workspaceUris;
    }

    @Override
    public Map<String, Pair<String, Integer>> findWorkspaceUrisForSpanIds(List<String> spanIds) {
        return PythonSpanNavigationProvider.getInstance(project).getUrisForSpanIds(spanIds);
    }

    @Override
    public Set<EndpointInfo> lookForDiscoveredEndpoints(String endpointId) {
        return Collections.emptySet();
    }

    @Override
    public void environmentChanged(Env newEnv, boolean refreshInsightsView) {
        if (refreshInsightsView) {
            EDT.ensureEDT(() -> {
                var fileEditor = FileEditorManager.getInstance(project).getSelectedEditor();
                if (fileEditor != null) {
                    var file = fileEditor.getFile();
                    if (VfsUtilsKt.isValidVirtualFile(file)) {
                        var psiFile = PsiManager.getInstance(project).findFile(file);
                        if (PsiUtils.isValidPsiFile(psiFile) && isRelevant(psiFile)) {
                            var selectedTextEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
                            if (selectedTextEditor != null) {
                                int offset = selectedTextEditor.getCaretModel().getOffset();
                                var methodUnderCaret = detectMethodUnderCaret(project, psiFile, selectedTextEditor, offset);
                                LatestMethodUnderCaretHolder.getInstance(project).saveLatestMethodUnderCaret(project, this, methodUnderCaret.getId());
                                CaretContextService.getInstance(project).contextChanged(methodUnderCaret);
                            }
                        }
                    }
                }
            });
        }

        PythonCodeLensService.getInstance(project).refreshCodeLens();
    }


    @Override
    public @NotNull DocumentInfo buildDocumentInfo(@NotNull PsiFile psiFile, BuildDocumentInfoProcessContext context) {
        Log.log(LOGGER::debug, "got buildDocumentInfo request for {}", psiFile);
        if (psiFile instanceof PyFile pyFile) {

            return ProgressManager.getInstance().runProcess(() -> Retries.retryWithResult(() -> ReadAction.compute(() -> PythonCodeObjectsDiscovery.buildDocumentInfo(project, pyFile)),
                    Throwable.class,50,5),new EmptyProgressIndicator());

        } else {
            Log.log(LOGGER::debug, "psi file is noy python, returning empty DocumentInfo for {}", psiFile);
            return new DocumentInfo(PsiUtils.psiFileToUri(psiFile), new HashMap<>());
        }
    }

    @Override
    public @NotNull DocumentInfo buildDocumentInfo(@NotNull PsiFile psiFile, @Nullable FileEditor newEditor, BuildDocumentInfoProcessContext context) {
        return buildDocumentInfo(psiFile, context);
    }


    @Override
    public boolean isRelevant(VirtualFile file) {

        return PsiAccessUtilsKt.runInReadAccessWithResult(() -> {
            if (file.isDirectory() || !VfsUtilsKt.isValidVirtualFile(file)) {
                return false;
            }

            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (PsiUtils.isValidPsiFile(psiFile)) {
                return false;
            }

            return isRelevant(psiFile);
        });
    }


    @Override
    public boolean isRelevant(PsiFile psiFile) {

        return SlowOperationsUtilsKt.allowSlowOperation(() -> PsiAccessUtilsKt.runInReadAccessWithResult(() -> psiFile.isValid() &&
                psiFile.isWritable() &&
                PythonLanguageUtils.isProjectFile(project, psiFile) &&
                !projectFileIndex.isInLibrary(psiFile.getVirtualFile()) &&
                !projectFileIndex.isExcluded(psiFile.getVirtualFile()) &&
                isSupportedFile(psiFile)));
    }

    @Override
    public void refreshMethodUnderCaret(@NotNull Project project, @NotNull PsiFile psiFile, @Nullable Editor selectedEditor, int offset) {
        MethodUnderCaret methodUnderCaret = detectMethodUnderCaret(project, psiFile, selectedEditor, offset);
        LatestMethodUnderCaretHolder.getInstance(project).saveLatestMethodUnderCaret(project, this, methodUnderCaret.getId());
        CaretContextService.getInstance(project).contextChanged(methodUnderCaret);
    }

    @Override
    public boolean isCodeVisionSupported() {
        return true;
    }

    @Override
    public @NotNull List<Pair<TextRange, CodeVisionEntry>> getCodeLens(@NotNull PsiFile psiFile) {
        return PythonCodeLensService.getInstance(project).getCodeLens(psiFile);
    }

    @Override
    public @Nullable PsiElement getPsiElementForMethod(@NotNull String methodId) {
        var functionName = PythonLanguageUtils.extractFunctionNameFromCodeObjectId(methodId);

        //try to find a function that produces the same code object id,
        // if found return its language, else null
        return ReadAction.compute(() -> {
            var functions = PyFunctionNameIndex.find(functionName, project, GlobalSearchScope.projectScope(project));

            for (PyFunction function : functions) {
                var codeObjectId = PythonLanguageUtils.createPythonMethodCodeObjectId(project, function);
                //python method has multiple ids, we don't know what the backend will send so check with all possible ids
                List<String> allIds = PythonAdditionalIdsProvider.getAdditionalIdsInclusive(codeObjectId, false);
                if (allIds.contains(methodId)) {
                    return function;
                }
            }

            return null;
        });
    }

    @Override
    public @Nullable PsiElement getPsiElementForClassByMethodId(@NotNull String methodId) {
        var functionName = PythonLanguageUtils.extractFunctionNameFromCodeObjectId(methodId);

        //try to find a function that produces the same code object id,
        // if found return its language, else null
        return ReadAction.compute(() -> {
            var functions = PyFunctionNameIndex.find(functionName, project, GlobalSearchScope.projectScope(project));

            for (PyFunction function : functions) {
                var codeObjectId = PythonLanguageUtils.createPythonMethodCodeObjectId(project, function);
                //python method has multiple ids, we don't know what the backend will send so check with all possible ids
                List<String> allIds = PythonAdditionalIdsProvider.getAdditionalIdsInclusive(codeObjectId, false);
                if (allIds.contains(methodId)) {
                    return PsiTreeUtil.getParentOfType(function, PyClass.class);
                }
            }

            return null;
        });
    }

    @Override
    public @Nullable PsiElement getPsiElementForClassByName(@NotNull String className) {
        return ReadAction.compute(() -> {
            var classes = PyClassNameIndex.find(className, project, GlobalSearchScope.projectScope(project));
            var cls = classes.stream().findFirst();
            return cls.orElse(null);
        });
    }

    @Override
    public @NotNull InstrumentationProvider getInstrumentationProvider() {
        return new NoOpInstrumentationProvider();
    }
}
