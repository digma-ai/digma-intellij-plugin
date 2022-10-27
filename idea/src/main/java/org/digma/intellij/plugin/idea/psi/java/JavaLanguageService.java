package org.digma.intellij.plugin.idea.psi.java;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.lang.Language;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import kotlin.Pair;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.digma.intellij.plugin.model.discovery.SpanInfo;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.psi.PsiUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class JavaLanguageService implements LanguageService {

    //used to find languages that should be indexed
    public static final FileType FILE_TYPE = JavaFileType.INSTANCE;

    public static String createJavaMethodCodeObjectId(String packageName, PsiClass aClass, PsiMethod method) {
        return packageName + "." + aClass.getName() + "$_$" + method.getName();
    }

    public static String createJavaMethodCodeObjectId(PsiMethod method) {

        //usually this should be non-null fields but in case they are we can't build the method id
        if (method.getContainingClass() == null || method.getContainingClass().getQualifiedName() == null) {
            return method.getName();
        }

        return method.getContainingClass().getQualifiedName() + "$_$" + method.getName();
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
        PsiMethod psiMethod = PsiTreeUtil.getParentOfType(underCaret, PsiMethod.class);
        if (psiMethod != null && psiMethod.getContainingClass() != null && psiMethod.getContainingClass().getName() != null) {
            return new MethodUnderCaret(createJavaMethodCodeObjectId(psiMethod), psiMethod.getName(),
                    psiMethod.getContainingClass().getName(), PsiUtils.psiFileToUri(psiFile));
        }
        return new MethodUnderCaret("", "", "", PsiUtils.psiFileToUri(psiFile), true);
    }

    @Override
    public void navigateToMethod(String codeObjectId) {

    }

    @Override
    public boolean isServiceFor(Language language) {
        return language.getClass().equals(JavaLanguage.class);
    }

    @Override
    public Map<String, String> findWorkspaceUrisForCodeObjectIds(List<String> codeObjectIds) {
        //todo: implement
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Pair<String, Integer>> findWorkspaceUrisForSpanIds(List<String> spanIds) {
        //todo: implement
        return Collections.emptyMap();
    }

    @Override
    public void environmentChanged(String newEnv) {
        //todo: implement
    }

    @Override
    public boolean isIndexedLanguage() {
        return true;
    }

    @Override
    public DocumentInfo buildDocumentInfo(PsiFile psiFile) {

        String fileUri = PsiUtils.psiFileToUri(psiFile);
        Map<String, MethodInfo> methodInfoMap = new HashMap<>();

        //it must be a PsiJavaFile so casting should be ok
        PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;

        String packageName = psiJavaFile.getPackageName();

        PsiClass[] classes = psiJavaFile.getClasses();

        for (PsiClass aClass : classes) {

            //don't collect methods for those types. if the file contains only those types then methodInfoMap
            //will be empty
            if (aClass.isInterface() || aClass.isAnnotationType() || aClass.isEnum() || aClass.isRecord()) {
                continue;
            }

            PsiMethod[] methods = aClass.getMethods();
            for (PsiMethod method : methods) {
                String id = createJavaMethodCodeObjectId(packageName, aClass, method);
                String name = method.getName();
                String containingClass = aClass.getQualifiedName();
                String containingNamespace = packageName;
                String containingFileUri = fileUri;
                int offsetAtFileUri = method.getTextOffset();
                List<SpanInfo> spans = new ArrayList<>();
                MethodInfo methodInfo = new MethodInfo(id, name, containingClass, containingNamespace, containingFileUri, offsetAtFileUri, spans);
                methodInfoMap.put(id, methodInfo);
            }
        }


        return new DocumentInfo(fileUri, methodInfoMap);
    }

    @Override
    public boolean isIntellijPlatformPluginLanguage() {
        return true;
    }

}
