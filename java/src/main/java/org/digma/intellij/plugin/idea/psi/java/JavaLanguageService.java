package org.digma.intellij.plugin.idea.psi.java;

import com.intellij.codeInsight.codeVision.CodeVisionEntry;
import com.intellij.lang.Language;
import com.intellij.lang.java.JavaLanguage;
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
import com.intellij.psi.*;
import com.intellij.psi.impl.java.stubs.index.JavaFullClassNameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import kotlin.Pair;
import org.digma.intellij.plugin.common.EDT;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.psi.PsiUtils;
import org.digma.intellij.plugin.ui.CaretContextService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.digma.intellij.plugin.idea.psi.java.JavaLanguageUtils.createJavaMethodCodeObjectId;

public class JavaLanguageService implements LanguageService {

    private static final Logger LOGGER = Logger.getInstance(JavaLanguageService.class);


    private final Project project;

    private final ProjectFileIndex projectFileIndex;

    private final MicronautFramework micronautFramework;
    private final JaxrsFramework jaxrsFramework;
    private final GrpcFramework grpcFramework;



    /*
    It's better, as much as possible, in language services especially, not to initialize service dependencies in the constructor but use
    a getInstance for services when they are first needed. that will minimize the possibility for cyclic dependencies.
     */
    public JavaLanguageService(Project project) {
        this.project = project;
        this.projectFileIndex = project.getService(ProjectFileIndex.class);
        this.micronautFramework = new MicronautFramework(project);
        this.jaxrsFramework = new JaxrsFramework(project);
        this.grpcFramework = new GrpcFramework(project);

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

        //try to parse the methodId as if it is java and try to find the language
        if (methodId.indexOf("$_$") <= 0){
            Log.log(LOGGER::debug, "method id in getLanguageForMethodCodeObjectId does not contain $_$ {}", methodId);
            return null;
        }

        var className = methodId.substring(0, methodId.indexOf("$_$"));

        //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
        className = className.replace('$', '.');

        var finalClassName = className;
        return ReadAction.compute(() -> {
            Collection<PsiClass> psiClasses =
                    JavaFullClassNameIndex.getInstance().get(finalClassName, project, GlobalSearchScope.projectScope(project));
            if (!psiClasses.isEmpty()) {
                Optional<PsiClass> psiClass = psiClasses.stream().findAny();
                //noinspection ConstantConditions
                if (psiClass.isPresent()) {
                    return psiClass.get().getLanguage();
                }
            }
            return null;
        });
    }


    @Override
    public boolean isSupportedFile(@NotNull Project project, @NotNull VirtualFile newFile) {
        //maybe more correct to find view provider and find a java psi file
        PsiFile psiFile = com.intellij.psi.PsiManager.getInstance(project).findFile(newFile);
        return psiFile != null && JavaLanguage.INSTANCE.equals(psiFile.getLanguage());
    }

    @Override
    public boolean isSupportedFile(@NotNull Project project, @NotNull PsiFile psiFile) {
        return JavaLanguage.INSTANCE.equals(psiFile.getLanguage());
    }

    @Override
    @NotNull
    public MethodUnderCaret detectMethodUnderCaret(@NotNull Project project, @NotNull PsiFile psiFile, @Nullable Editor selectedEditor, int caretOffset) {
        if (!isSupportedFile(project,psiFile)){
            return new MethodUnderCaret("", "", "", PsiUtils.psiFileToUri(psiFile), false);
        }
        PsiElement underCaret =  psiFile.findElementAt(caretOffset);
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


    /**
     * Navigate to any method in the project even if the file is not opened
     */
    @Override
    public void navigateToMethod(String methodId) {

        Log.log(LOGGER::debug, "got navigate to method request {}", methodId);
        if (methodId.indexOf("$_$") <= 0){
            Log.log(LOGGER::debug, "method id in navigateToMethod does not contain $_$, can not navigate {}", methodId);
            return;
        }

        var className = methodId.substring(0, methodId.indexOf("$_$"));

        //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
        className = className.replace('$', '.');

        //searching in project scope will find only project classes
        Collection<PsiClass> psiClasses =
                JavaFullClassNameIndex.getInstance().get(className, project, GlobalSearchScope.allScope(project));
        if (!psiClasses.isEmpty()) {
            //hopefully there is only one class by that name in the project
            Optional<PsiClass> psiClassOptional = psiClasses.stream().findAny();
            PsiClass psiClass = psiClassOptional.get();
            for (PsiMethod method : psiClass.getMethods()) {
                var id = JavaLanguageUtils.createJavaMethodCodeObjectId(method);
                if (id.equals(methodId) && method.canNavigate()) {
                    Log.log(LOGGER::debug, "navigating to method {}", method);
                    method.navigate(true);
                    return;
                }
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
    public boolean isServiceFor(@NotNull Language language) {
        return JavaLanguage.class.equals(language.getClass());
    }

    @Override
    public Map<String, String> findWorkspaceUrisForCodeObjectIdsForErrorStackTrace(List<String> codeObjectIds) {

        /*
        Use intellij index to find in which file a method exists.
        We don't need to search for the method, only for the class. and we don't need offset because
        the backend provides a line number to navigate to.

        We could use the class name that is returned from the backend in org.digma.intellij.plugin.model.rest.errordetails.Frame.
        but then we need to change the C# implementation too.
        but codeObjectIds is usually our identifier all over the plugin, so maybe it's better to just use it for all languages.
        the string parsing of codeObjectIds should not be expensive.
         */


        Map<String, String> workspaceUrls = new HashMap<>();

        codeObjectIds.forEach(methodId -> {

            if (methodId.contains("$_$")) {
                var className = methodId.substring(0, methodId.indexOf("$_$"));

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
                        workspaceUrls.put(methodId, url);
                    }
                }
            }else{
                Log.log(LOGGER::debug, "method id in findWorkspaceUrisForCodeObjectIdsForErrorStackTrace does not contain $_$ {}", methodId);
            }
        });

        return workspaceUrls;
    }

    @Override
    public Map<String, Pair<String, Integer>> findWorkspaceUrisForMethodCodeObjectIds(List<String> methodCodeObjectIds) {

        Map<String, Pair<String, Integer>> workspaceUrls = new HashMap<>();

        methodCodeObjectIds.forEach(methodId -> {

            if (methodId.contains("$_$")) {
                var className = methodId.substring(0, methodId.indexOf("$_$"));

                //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
                className = className.replace('$', '.');

                //searching in project scope will find only project classes
                Collection<PsiClass> psiClasses =
                        JavaFullClassNameIndex.getInstance().get(className, project, GlobalSearchScope.projectScope(project));
                if (!psiClasses.isEmpty()) {
                    //hopefully there is only one class by that name in the project
                    PsiClass psiClass = psiClasses.stream().findAny().get();
                    PsiFile psiFile = PsiTreeUtil.getParentOfType(psiClass, PsiFile.class);
                    for (PsiMethod method : psiClass.getMethods()) {
                        String javaMethodCodeObjectId = createJavaMethodCodeObjectId(method);
                        if (javaMethodCodeObjectId.equals(methodId) && psiFile != null) {
                            String url = PsiUtils.psiFileToUri(psiFile);
                            workspaceUrls.put(methodId, new Pair<>(url, method.getTextOffset()));

                        }
                    }
                }
            }else{
                Log.log(LOGGER::debug, "method id in findWorkspaceUrisForMethodCodeObjectIds does not contain $_$ {}", methodId);
            }
        });

        return workspaceUrls;
    }


    @NotNull
    @Override
    public Map<String, Pair<String, Integer>> findWorkspaceUrisForSpanIds(@NotNull List<String> spanIds) {
        return JavaSpanNavigationProvider.getInstance(project).getUrisForSpanIds(spanIds);
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
                        var methodUnderCaret = detectMethodUnderCaret(project, psiFile, selectedTextEditor, offset);
                        CaretContextService.getInstance(project).contextChanged(methodUnderCaret);
                    }
                }
            }
        });

        JavaCodeLensService.getInstance(project).environmentChanged(newEnv);
    }




    @Override
    public @NotNull DocumentInfo buildDocumentInfo(@NotNull PsiFile psiFile) {
        Log.log(LOGGER::debug, "got buildDocumentInfo request for {}", psiFile);
        //must be PsiJavaFile , this method should be called only for java files
        if (psiFile instanceof PsiJavaFile psiJavaFile) {
            return JavaCodeObjectDiscovery.buildDocumentInfo(project, psiJavaFile, micronautFramework, jaxrsFramework, grpcFramework);
        }else{
            Log.log(LOGGER::debug, "psi file is noy java, returning empty DocumentInfo for {}", psiFile);
            return new DocumentInfo(PsiUtils.psiFileToUri(psiFile), new HashMap<>());
        }
    }

    @Override
    public @NotNull DocumentInfo buildDocumentInfo(@NotNull PsiFile psiFile, @Nullable FileEditor newEditor) {
        return buildDocumentInfo(psiFile);
    }


    @Override
    public boolean isRelevant(VirtualFile file) {

        if (file.isDirectory() || !file.isValid()){
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
                projectFileIndex.isInSourceContent(psiFile.getVirtualFile()) &&
                !projectFileIndex.isInLibrary(psiFile.getVirtualFile()) &&
                !projectFileIndex.isExcluded(psiFile.getVirtualFile()) &&
                isSupportedFile(project, psiFile) &&
                !JavaDocumentInfoIndex.namesToExclude.contains(psiFile.getVirtualFile().getName());
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
        return JavaCodeLensService.getInstance(project).getCodeLens(psiFile);
    }
}
