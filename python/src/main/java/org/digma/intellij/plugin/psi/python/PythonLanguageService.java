package org.digma.intellij.plugin.psi.python;

import com.intellij.codeInsight.codeVision.CodeVisionEntry;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.PythonLanguage;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.psi.PyFunction;
import com.jetbrains.python.psi.stubs.PyFunctionNameIndex;
import kotlin.Pair;
import org.digma.intellij.plugin.common.EDT;
import org.digma.intellij.plugin.common.ReadActions;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.psi.PsiUtils;
import org.digma.intellij.plugin.ui.CaretContextService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        if (DumbService.isDumb(project)){
            DumbService.getInstance(project).runWhenSmart(task);
        }else{
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
            var functions = PyFunctionNameIndex.find(functionName, project,GlobalSearchScope.projectScope(project));

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
    public boolean isSupportedFile(@NotNull Project project, @NotNull VirtualFile newFile) {
        PsiFile psiFile = com.intellij.psi.PsiManager.getInstance(project).findFile(newFile);
        return psiFile != null && PythonLanguage.INSTANCE.equals(psiFile.getLanguage());
    }

    @Override
    public boolean isSupportedFile(@NotNull Project project, @NotNull PsiFile psiFile) {
        return PythonLanguage.INSTANCE.equals(psiFile.getLanguage());
    }

    @Override
    @NotNull
    public MethodUnderCaret detectMethodUnderCaret(@NotNull Project project, @NotNull PsiFile psiFile, Editor selectedEditor, int caretOffset) {
        if (!isSupportedFile(project, psiFile)) {
            return new MethodUnderCaret("", "", "", "",PsiUtils.psiFileToUri(psiFile), false);
        }
        PsiElement underCaret = psiFile.findElementAt(caretOffset);
        if (underCaret == null) {
            return new MethodUnderCaret("", "", "","", PsiUtils.psiFileToUri(psiFile), true);
        }
        PyFunction pyFunction = PsiTreeUtil.getParentOfType(underCaret, PyFunction.class);
        if (pyFunction != null) {
            var methodId = PythonLanguageUtils.createPythonMethodCodeObjectId(project, pyFunction);
            var name = pyFunction.getName() == null ? "" : pyFunction.getName();
            var containingClass = pyFunction.getContainingClass();
            var className = containingClass == null ? "" : containingClass.getName() + ".";
            return new MethodUnderCaret(methodId, name, className, "", PsiUtils.psiFileToUri(psiFile));
        }
        return new MethodUnderCaret("", "", "", "", PsiUtils.psiFileToUri(psiFile), true);
    }


    /**
     * navigate to a method. this method is meant to be used only to navigate to a method in the current selected editor.
     * it is used from the methods preview list. it will not navigate to any method in the project.
     */
    @Override
    public void navigateToMethod(String methodId) {

        Log.log(LOGGER::debug, "got navigate to method request {}", methodId);

        var functionName = PythonLanguageUtils.extractFunctionNameFromCodeObjectId(methodId);

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
                var selectedEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
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
    public void environmentChanged(String newEnv, boolean refreshInsightsView) {
        if (refreshInsightsView) {
            EDT.ensureEDT(() -> {
                var fileEditor = FileEditorManager.getInstance(project).getSelectedEditor();
                if (fileEditor != null) {
                    var file = fileEditor.getFile();
                    var psiFile = PsiManager.getInstance(project).findFile(file);
                    if (psiFile != null && isRelevant(psiFile)) {
                        var selectedTextEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
                        if (selectedTextEditor != null) {
                            int offset = selectedTextEditor.getCaretModel().getOffset();
                            var methodUnderCaret = detectMethodUnderCaret(project, psiFile, selectedTextEditor, offset);
                            CaretContextService.getInstance(project).contextChanged(methodUnderCaret);
                        }
                    }
                }
            });
        }

        PythonCodeLensService.getInstance(project).environmentChanged(newEnv);
    }


    @Override
    public @NotNull DocumentInfo buildDocumentInfo(@NotNull PsiFile psiFile) {
        Log.log(LOGGER::debug, "got buildDocumentInfo request for {}", psiFile);
        if (psiFile instanceof PyFile pyFile) {
            return PythonCodeObjectsDiscovery.buildDocumentInfo(project, pyFile);
        }else {
            Log.log(LOGGER::debug, "psi file is noy python, returning empty DocumentInfo for {}", psiFile);
            return new DocumentInfo(PsiUtils.psiFileToUri(psiFile), new HashMap<>());
        }
    }

    @Override
    public @NotNull DocumentInfo buildDocumentInfo(@NotNull PsiFile psiFile, @Nullable FileEditor newEditor) {
        return buildDocumentInfo(psiFile);
    }


    @Override
    public boolean isRelevant(VirtualFile file) {

        if (file.isDirectory() || !file.isValid()) {
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
                PythonLanguageUtils.isProjectFile(project, psiFile) &&
                !projectFileIndex.isInLibrary(psiFile.getVirtualFile()) &&
                !projectFileIndex.isExcluded(psiFile.getVirtualFile()) &&
                isSupportedFile(project, psiFile);
    }

    @Override
    public void refreshMethodUnderCaret(@NotNull Project project, @NotNull PsiFile psiFile, @Nullable Editor selectedEditor, int offset) {
        MethodUnderCaret methodUnderCaret = detectMethodUnderCaret(project, psiFile, selectedEditor, offset);
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


}
