package org.digma.intellij.plugin.pycharm.psi.python;

import com.intellij.lang.Language;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
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


    private final Project project;

    private final DocumentInfoService documentInfoService;

    private final ProjectFileIndex projectFileIndex;

    private final CaretContextService caretContextService;

    public PythonLanguageService(Project project) {
        this.project = project;
        documentInfoService = project.getService(DocumentInfoService.class);
        projectFileIndex = project.getService(ProjectFileIndex.class);
        caretContextService = project.getService(CaretContextService.class);
    }


    @Override
    public void ensureStartup(@NotNull Project project) {
        //nothing to do
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
            return new MethodUnderCaret("", "", "", PsiUtils.psiFileToUri(psiFile), false);
        }
        PsiElement underCaret = psiFile.findElementAt(caretOffset);
        if (underCaret == null) {
            return new MethodUnderCaret("", "", "", PsiUtils.psiFileToUri(psiFile), true);
        }
        PyFunction pyFunction = PsiTreeUtil.getParentOfType(underCaret, PyFunction.class);
        if (pyFunction != null) {
            var methodId = PythonLanguageUtils.createPythonMethodCodeObjectId(project, pyFunction);
            var name = pyFunction.getName() == null ? "" : pyFunction.getName();
            var containingClass = pyFunction.getContainingClass();
            var className = containingClass == null ? "" : containingClass.getName() + ".";
            return new MethodUnderCaret(methodId, name, className, PsiUtils.psiFileToUri(psiFile));
        }
        return new MethodUnderCaret("", "", "", PsiUtils.psiFileToUri(psiFile), true);
    }


    /**
     * navigate to a method. this method is meant to be used only to navigate to a method in the current selected editor.
     * it is used from the methods preview list. it will not navigate to any method in the project.
     */
    @Override
    public void navigateToMethod(String codeObjectId) {

        /*
        There are few ways to navigate to a method.
        the current implementation is the simplest, maybe not the best in performance, but it doesn't seem to be noticed.
        find the psi file in documentInfoService , then find the psi method and call psiMethod.navigate.

        another option is to search for the method in intellij index and navigate.
         */


        PsiFile psiFile = documentInfoService.findPsiFileByMethodId(codeObjectId);
        if (psiFile instanceof PyFile pyFile) {

            PyFunction pyFunction = PythonLanguageUtils.findMethodInFile(project, pyFile, codeObjectId);

            if (pyFunction != null && pyFunction.canNavigateToSource()) {
                pyFunction.navigate(true);
            } else if (pyFunction != null) {
                //it's a fallback. sometimes the psiMethod.canNavigateToSource is false and really the
                //navigation doesn't work. i can't say why. usually it happens when indexing is not ready yet,
                // and the user opens files, selects tabs or moves the caret. then when indexing is finished
                // we have the list of methods but then psiMethod.navigate doesn't work.
                // navigation to source using the editor does work in these circumstances.
                var selectedEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
                if (selectedEditor != null) {
                    selectedEditor.getCaretModel().moveToOffset(pyFunction.getTextOffset());
                } else {
                    Log.log(LOGGER::error, "could not find selected text editor, can't navigate to method  {}", codeObjectId);
                }
            } else {
                Log.log(LOGGER::error, "could not navigate to method {}, can't find PsiMethod in file {}", codeObjectId, psiFile.getVirtualFile());
            }
        }
    }


    @Override
    public boolean isServiceFor(@NotNull Language language) {
        return PythonLanguage.class.equals(language.getClass());
    }

    @Override
    public Map<String, String> findWorkspaceUrisForCodeObjectIds(List<String> codeObjectIds) {

        var workspaceUris = new HashMap<String, String>();

        codeObjectIds.forEach(methodId -> {

            var functionName = PythonLanguageUtils.extractFunctionNameFromCodeObjectId(methodId);

            var functions = PyFunctionNameIndex.find(functionName, project, GlobalSearchScope.projectScope(project));

            for (PyFunction function : functions) {
                if (function.isValid()) {
                    PsiFile psiFile = function.getContainingFile();
                    if (PythonLanguageUtils.isProjectFile(project, psiFile)) {
                        var codeObjectId = PythonLanguageUtils.createPythonMethodCodeObjectId(project, function);
                        List<String> allIds = PythonAdditionalIdsProvider.getAdditionalIdsInclusive(codeObjectId, false);
                        if (allIds.contains(methodId)) {
                            String url = PsiUtils.psiFileToUri(psiFile);
                            workspaceUris.put(codeObjectId, url);
                        }
                    }
                }
            }
        });

        return workspaceUris;
    }

    @Override
    public Map<String, Pair<String, Integer>> findWorkspaceUrisForSpanIds(List<String> spanIds) {
        return PythonSpanNavigationProvider.getInstance(project).getUrisForSpanIds(spanIds);
    }

    @Override
    public void environmentChanged(String newEnv) {

        EDT.ensureEDT(() -> {
            var fileEditor = FileEditorManager.getInstance(project).getSelectedEditor();
            if (fileEditor != null) {
                var file = fileEditor.getFile();
                var psiFile = PsiManager.getInstance(project).findFile(file);
                if (psiFile != null && isRelevant(psiFile)) {
                    var selectedTextEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
                    if (selectedTextEditor != null) {
                        int offset = selectedTextEditor.getCaretModel().getOffset();
                        var methodUnderCaret = detectMethodUnderCaret(project, psiFile, null, offset);
                        caretContextService.contextChanged(methodUnderCaret);
                    }
                }
            }
        });

        PythonCodeLensService.getInstance(project).environmentChanged(newEnv);
    }


    @Override
    public @NotNull DocumentInfo buildDocumentInfo(@NotNull PsiFile psiFile) {
        if (psiFile instanceof PyFile pyFile) {
            return PythonCodeObjectsDiscovery.buildDocumentInfo(project, pyFile);
        }
        return new DocumentInfo(PsiUtils.psiFileToUri(psiFile), new HashMap<>());
    }

    @Override
    public @NotNull DocumentInfo buildDocumentInfo(@NotNull PsiFile psiFile, @Nullable FileEditor newEditor) {
        return buildDocumentInfo(psiFile);
    }

    @Override
    public boolean isIntellijPlatformPluginLanguage() {
        return true;
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
        caretContextService.contextChanged(methodUnderCaret);
    }


}
