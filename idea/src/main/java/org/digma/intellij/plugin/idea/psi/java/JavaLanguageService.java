package org.digma.intellij.plugin.idea.psi.java;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.lang.Language;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.java.stubs.index.JavaFullClassNameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import kotlin.Pair;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.index.DocumentInfoIndex;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.digma.intellij.plugin.model.discovery.SpanInfo;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.psi.PsiUtils;
import org.digma.intellij.plugin.ui.CaretContextService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class JavaLanguageService implements LanguageService {

    private static final Logger LOGGER = Logger.getInstance(JavaLanguageService.class);

    //used to find languages that should be indexed.
    // (used with reflection so intellij can't find usages and reports the field is not used)
    public static final FileType FILE_TYPE = JavaFileType.INSTANCE;
    private final Project project;

    private final DocumentInfoService documentInfoService;

    private final CaretContextService caretContextService;

    public JavaLanguageService(Project project) {
        this.project = project;
        documentInfoService = project.getService(DocumentInfoService.class);
        caretContextService = project.getService(CaretContextService.class);
    }


    public static String createJavaMethodCodeObjectId(PsiMethod method) {

        //usually these should be not null for java. but the PSI api declares them as null so we must check.
        // they can be null for groovy/kotlin
        if (method.getContainingClass() == null || method.getContainingClass().getQualifiedName() == null) {
            return method.getName();
        }

        var packageName = ((PsiJavaFile) method.getContainingFile()).getPackageName();

        var className = "";
        try {
            className = method.getContainingClass().getQualifiedName().substring(packageName.length() + 1).replace('.', '$');
        } catch (NullPointerException e) {
            //there should not be a NPE for java method because in java a method must have a containing class.
            // It's only to satisfy intellij warnings.
            //the methods getContainingClass and getQualifiedName may return null, but it can only happen
            //for other jvm languages like scala/groovy/kotlin
        }

        return packageName + "." + className + "$_$" + method.getName();
    }


    @Override
    public Language getLanguageForMethodCodeObjectId(@NotNull String methodId) {

        //try to parse the methodId as if it is java and try to find the language

        var className = methodId.substring(0, methodId.indexOf("$_$"));

        //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
        className = className.replace('$', '.');

        //searching in project scope will find only project classes
        Collection<PsiClass> psiClasses =
                JavaFullClassNameIndex.getInstance().get(className, project, GlobalSearchScope.projectScope(project));
        //todo: maybe also search in method index and compare to find the same file
        // or try to verify that this class really has the method
        if (!psiClasses.isEmpty()) {
            PsiClass psiClass = psiClasses.stream().findAny().get();
            return psiClass.getLanguage();
        }

        return null;
    }


    @Override
    public boolean isSupportedFile(Project project, VirtualFile newFile) {
        //maybe more correct to find view provider and find a java psi file
        PsiFile psiFile = com.intellij.psi.PsiManager.getInstance(project).findFile(newFile);
        if (psiFile == null) {
            return false;
        }
        return JavaLanguage.INSTANCE.equals(psiFile.getLanguage());
    }

    @Override
    public boolean isSupportedFile(Project project, PsiFile psiFile) {
        return JavaLanguage.INSTANCE.equals(psiFile.getLanguage());
    }

    @Override
    public MethodUnderCaret detectMethodUnderCaret(@NotNull Project project, @NotNull PsiFile psiFile, int caretOffset) {
        PsiElement underCaret = findElementUnderCaret(project, psiFile, caretOffset);
        if (underCaret == null) {
            return new MethodUnderCaret("", "", "", PsiUtils.psiFileToUri(psiFile), true);
        }
        PsiMethod psiMethod = PsiTreeUtil.getParentOfType(underCaret, PsiMethod.class);
        if (psiMethod != null && psiMethod.getContainingClass() != null && psiMethod.getContainingClass().getName() != null) {
            return new MethodUnderCaret(createJavaMethodCodeObjectId(psiMethod), psiMethod.getName(),
                    psiMethod.getContainingClass().getName(), PsiUtils.psiFileToUri(psiFile));
        }
        return new MethodUnderCaret("", "", "", PsiUtils.psiFileToUri(psiFile), true);
    }


    @Override
    public void navigateToMethod(String codeObjectId) {

        /*
        There are few ways to navigate to a method.
        the current implementation is the simplest, maybe not the best in performance but it doesn't seem to be noticed.
        find the psi file in documentInfoService , then find the psi method and call psiMethod.navigate.
        it proves to work ok for java files.

        other possibilities:
        1)
        we have the method offset of the method in our MethodInfo, so we can find the MethodInfo in
        documentInfoService. or the MethodInfo object can also be in listViewItem.moreData of the PreviewListCellRenderer.
        then find the selected editor using FileEditorManager.getSelectedTextEditor
        then selectedTextEditor.getCaretModel().moveToOffset();

        2)
        another way is to find the psi file through the selected editor then do the same , find the method and navigate.

        another way could be to build an index of codeObjectId -> [an object that holds the file uri and method offset],
        then find that object in the index and use the offset to move the caret in the selected editor

        if the current implementation proves to be slow or not reliable we can try one of the other options.
         */


        PsiFile psiFile = documentInfoService.findPsiFileByMethodId(codeObjectId);
        if (psiFile instanceof PsiJavaFile) {

            //it must be a PsiJavaFile so casting should be ok
            PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;
            PsiClass[] classes = psiJavaFile.getClasses();
            PsiMethod psiMethod = findMethod(classes, codeObjectId);

            if (psiMethod != null && psiMethod.canNavigateToSource()) {
                psiMethod.navigate(true);
            } else if (psiMethod != null) {
                //it's a fallback. sometimes the psiMethod.canNavigateToSource is false and really the
                //navigation doesn't work. i can't say why. usually it happens when indexxing is not ready yet,
                // and the user opens files, selects tabs or moves the caret. then when indexing is finished
                // we have the list of methods but then psiMethod.navigate doesn't work.
                // navigation to source using the editor does work in these circumstances.
                var selectedEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
                if (selectedEditor != null) {
                    selectedEditor.getCaretModel().moveToOffset(psiMethod.getTextOffset());
                } else {
                    Log.log(LOGGER::error, "could not find selected text editor, can't navigate to method  {}", codeObjectId);
                }
            } else {
                Log.log(LOGGER::error, "could not navigate to method {}, can't find PsiMethod in file {}", codeObjectId, psiFile.getVirtualFile());
            }
        }
    }

    @Nullable
    private PsiMethod findMethod(PsiClass[] classes, String codeObjectId) {

        for (PsiClass aClass : classes) {
            PsiMethod[] methods = aClass.getMethods();
            for (PsiMethod method : methods) {
                String id = createJavaMethodCodeObjectId(method);
                if (id.equals(codeObjectId)) {
                    return method;
                }
            }

            var m = findMethod(aClass.getInnerClasses(), codeObjectId);
            if (m != null) {
                return m;
            }
        }

        return null;
    }


    @Override
    public boolean isServiceFor(Language language) {
        return language.getClass().equals(JavaLanguage.class);
    }

    @Override
    public Map<String, String> findWorkspaceUrisForCodeObjectIds(List<String> codeObjectIds) {

        /*
        Use intellij index to find in which file a method exists.
        We don't need to search for the method, only for the class. and we don't need offset because
        the backend provides a line number to navigate to.

        We could use the class name that is returned from the backend in org.digma.intellij.plugin.model.rest.errordetails.Frame.
        but then we need to change the C# implementation too.
        but codeObjectIds is usually our identifier all over the plugin so maybe its better to just use it for all languages.
        the string parsing of codeObjectIds should not be expensive.
         */


        Map<String, String> workspaceUrls = new HashMap<>();

        codeObjectIds.forEach(id -> {

            var className = id.substring(0, id.indexOf("$_$"));

            //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
            className = className.replace('$', '.');

            //searching in project scope will find only project classes
            Collection<PsiClass> psiClasses =
                    JavaFullClassNameIndex.getInstance().get(className, project, GlobalSearchScope.projectScope(project));
            if (!psiClasses.isEmpty()) {
                //hopefully there is only one class by that name in the project
                PsiClass psiClass = psiClasses.stream().findAny().get();
                PsiFile psiFile = PsiTreeUtil.getParentOfType(psiClass, PsiFile.class);
                if (psiFile != null) {
                    String url = PsiUtils.psiFileToUri(psiFile);
                    workspaceUrls.put(id, url);
                }
            }
        });

        return workspaceUrls;
    }


    @Override
    public Map<String, Pair<String, Integer>> findWorkspaceUrisForSpanIds(List<String> spanIds) {
        //todo: implement
        return Collections.emptyMap();
    }

    @Override
    public void environmentChanged(String newEnv) {

        ApplicationManager.getApplication().invokeAndWait(() -> {
            var fileEditor = FileEditorManager.getInstance(project).getSelectedEditor();
            if (fileEditor != null) {
                var file = fileEditor.getFile();
                var psiFile = PsiManager.getInstance(project).findFile(file);
                if (psiFile != null && isRelevantFile(psiFile)) {
                    var selectedTextEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
                    if (selectedTextEditor != null) {
                        int offset = selectedTextEditor.getCaretModel().getOffset();
                        var methodUnderCaret = detectMethodUnderCaret(project, psiFile, offset);
                        caretContextService.contextChanged(methodUnderCaret);
                    }
                }
            }
        });
    }


    private boolean isRelevantFile(PsiFile psiFile) {
        //if file is not writable it is not supported even if it's a language we support, usually when we open vcs files.
        return psiFile.isWritable() &&
                isSupportedFile(project, psiFile) &&
                !ProjectFileIndex.getInstance(project).isInLibrary(psiFile.getVirtualFile()) &&
                !ProjectFileIndex.getInstance(project).isInTestSourceContent(psiFile.getVirtualFile()) &&
                !DocumentInfoIndex.namesToExclude.contains(psiFile.getVirtualFile().getName());
    }


    @Override
    public boolean isIndexedLanguage() {
        return true;
    }

    @Override
    public DocumentInfo buildDocumentInfo(PsiFile psiFile) {

        String fileUri = PsiUtils.psiFileToUri(psiFile);
        Map<String, MethodInfo> methodInfoMap = new HashMap<>();


        //currently we build an empty index for test sources, there is no easy way to exclude them from indexing
        if (ProjectFileIndex.getInstance(project).isInTestSourceContent(psiFile.getVirtualFile())) {
            return new DocumentInfo(fileUri, methodInfoMap);
        }

        //it must be a PsiJavaFile so casting should be ok
        PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;

        String packageName = psiJavaFile.getPackageName();

        PsiClass[] classes = psiJavaFile.getClasses();

        collectMethods(fileUri, classes, packageName, methodInfoMap);

        return new DocumentInfo(fileUri, methodInfoMap);
    }

    private void collectMethods(String fileUri, PsiClass[] classes, String packageName, Map<String, MethodInfo> methodInfoMap) {

        for (PsiClass aClass : classes) {

            //don't collect methods for those types. if the file contains only those types then methodInfoMap
            //will be empty
            if (aClass.isInterface() || aClass.isAnnotationType() || aClass.isEnum() || aClass.isRecord()) {
                continue;
            }

            PsiMethod[] methods = aClass.getMethods();
            for (PsiMethod method : methods) {
                String id = createJavaMethodCodeObjectId(method);
                String name = method.getName();
                String containingClass = aClass.getQualifiedName();
                String containingNamespace = packageName;
                String containingFileUri = fileUri;
                int offsetAtFileUri = method.getTextOffset();
                List<SpanInfo> spans = new ArrayList<>();
                MethodInfo methodInfo = new MethodInfo(id, name, containingClass, containingNamespace, containingFileUri, offsetAtFileUri, spans);
                methodInfoMap.put(id, methodInfo);
            }

            collectMethods(fileUri, aClass.getInnerClasses(), packageName, methodInfoMap);
        }

    }


    @Override
    public boolean isIntellijPlatformPluginLanguage() {
        return true;
    }

}
