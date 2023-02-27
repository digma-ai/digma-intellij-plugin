package org.digma.intellij.plugin.idea.psi.java;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.util.Query;
import org.apache.commons.lang3.time.StopWatch;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.discovery.SpanInfo;
import org.digma.intellij.plugin.psi.PsiUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.digma.intellij.plugin.idea.psi.java.Constants.SPAN_BUILDER_FQN;
import static org.digma.intellij.plugin.idea.psi.java.JavaLanguageUtils.createJavaMethodCodeObjectId;
import static org.digma.intellij.plugin.idea.psi.java.JavaSpanDiscoveryUtils.filterNonRelevantMethodsForSpanDiscovery;
import static org.digma.intellij.plugin.idea.psi.java.JavaSpanDiscoveryUtils.filterNonRelevantReferencesForSpanDiscovery;

@SuppressWarnings("UnstableApiUsage")
public class JavaCodeObjectDiscovery {

    private static final Logger LOGGER = Logger.getInstance(JavaCodeObjectDiscovery.class);


    public static @NotNull DocumentInfo buildDocumentInfo(@NotNull Project project, @NotNull PsiJavaFile psiJavaFile) {
        var stopWatch = StopWatch.createStarted();

        try {
            return buildDocumentInfoImpl(project, psiJavaFile);
        } finally {
            stopWatch.stop();
            Log.log(LOGGER::debug, "buildDocumentInfo for {} took {} milliseconds", psiJavaFile.getName(), stopWatch.getTime(TimeUnit.MILLISECONDS));
        }
    }

    private static DocumentInfo buildDocumentInfoImpl(@NotNull Project project, @NotNull PsiJavaFile psiJavaFile) {
        String fileUri = PsiUtils.psiFileToUri(psiJavaFile);
        Map<String, MethodInfo> methodInfoMap = new HashMap<>();


        //currently we build an empty index for test sources, there is no easy way to exclude them from indexing
        if (ProjectFileIndex.getInstance(project).isInTestSourceContent(psiJavaFile.getVirtualFile())) {
            return new DocumentInfo(fileUri, methodInfoMap);
        }


        String packageName = psiJavaFile.getPackageName();

        PsiClass[] classes = psiJavaFile.getClasses();

        collectMethods(fileUri, classes, packageName, methodInfoMap);

        return new DocumentInfo(fileUri, methodInfoMap);
    }


    private static void collectMethods(@NotNull String fileUri, @NotNull PsiClass[] classes, @NotNull String packageName, @NotNull Map<String, MethodInfo> methodInfoMap) {

        for (PsiClass aClass : classes) {

            //don't collect methods for those types. if the file contains only those types then methodInfoMap
            //will be empty
            if (aClass.isAnnotationType() || aClass.isEnum() || aClass.isRecord()) {
                continue;
            }

            PsiMethod[] methods = aClass.getMethods();
            for (PsiMethod method : methods) {
                String id = createJavaMethodCodeObjectId(method);
                String name = method.getName();
                String containingClassName = aClass.getQualifiedName();
                @SuppressWarnings("UnnecessaryLocalVariable")
                String containingNamespace = packageName;
                @SuppressWarnings("UnnecessaryLocalVariable")
                String containingFileUri = fileUri;
                int offsetAtFileUri = method.getTextOffset();
                List<SpanInfo> spans = new ArrayList<>();
                Objects.requireNonNull(containingClassName, "a class in java must have a qualified name");
                MethodInfo methodInfo = new MethodInfo(id, name, containingClassName, containingNamespace, containingFileUri, offsetAtFileUri, spans);
                methodInfoMap.put(id, methodInfo);
            }

            collectMethods(fileUri, aClass.getInnerClasses(), packageName, methodInfoMap);
        }
    }


    static void enrichDocumentInfo(Project project, @NotNull DocumentInfo documentInfo, @NotNull PsiFile psiFile, @NotNull MicronautFramework micronautFramework, @NotNull JaxrsFramework jaxrsFramework, @NotNull GrpcFramework grpcFramework) {
         /*
        This method is called after loading the DocumentInfo from DocumentInfoIndex, and it is meant to
        enrich the DocumentInfo with discovery that can not be done in file based index or dumb mode.
        for example span discovery does not work in dumb mode, it must be done in smart mode.
        This method must be called in smart mode inside s ReadAction or UI thread.
         */

        spanDiscovery(project, psiFile, documentInfo);
        endpointDiscovery(psiFile, documentInfo, micronautFramework, jaxrsFramework, grpcFramework);
    }

    private static void spanDiscovery(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull DocumentInfo documentInfo) {
        Log.log(LOGGER::debug, "Building spans for file {}", psiFile);
        withSpanAnnotationSpanDiscovery(project, psiFile, documentInfo);
        startSpanMethodCallSpanDiscovery(project, psiFile, documentInfo);
    }

    private static void endpointDiscovery(@NotNull PsiFile psiFile, @NotNull DocumentInfo documentInfo, @NotNull MicronautFramework micronautFramework, @NotNull JaxrsFramework jaxrsFramework, @NotNull GrpcFramework grpcFramework) {
        Log.log(LOGGER::debug, "Building endpoints for file {}", psiFile);
        micronautFramework.endpointDiscovery(psiFile, documentInfo);
        jaxrsFramework.endpointDiscovery(psiFile, documentInfo);
        grpcFramework.endpointDiscovery(psiFile, documentInfo);
    }


    private static void startSpanMethodCallSpanDiscovery(Project project, @NotNull PsiFile psiFile, @NotNull DocumentInfo documentInfo) {

        PsiClass tracerBuilderClass = JavaPsiFacade.getInstance(project).findClass(SPAN_BUILDER_FQN, GlobalSearchScope.allScope(project));
        if (tracerBuilderClass != null) {
            PsiMethod startSpanMethod =
                    JavaLanguageUtils.findMethodInClass(tracerBuilderClass, "startSpan", psiMethod -> psiMethod.getParameters().length == 0);
            Objects.requireNonNull(startSpanMethod, "startSpan method must be found in SpanBuilder class");

            Query<PsiReference> startSpanReferences = MethodReferencesSearch.search(startSpanMethod, GlobalSearchScope.fileScope(psiFile), true);
            //filter classes that we don't support,which should not happen but just in case. we don't support Annotations,Enums and Records.
            startSpanReferences = filterNonRelevantReferencesForSpanDiscovery(startSpanReferences);

            startSpanReferences.forEach(psiReference -> {
                SpanInfo spanInfo = JavaSpanDiscoveryUtils.getSpanInfoFromStartSpanMethodReference(project, psiReference);
                if (spanInfo != null) {
                    Log.log(LOGGER::debug, "Found span info {} for method {}", spanInfo.getId(), spanInfo.getContainingMethodId());
                    MethodInfo methodInfo = documentInfo.getMethods().get(spanInfo.getContainingMethodId());
                    //this method must exist in the document info
                    Objects.requireNonNull(methodInfo, "method info " + spanInfo.getContainingMethodId() + " must exist in DocumentInfo for " + documentInfo.getFileUri());
                    methodInfo.getSpans().add(spanInfo);
                }
            });
        }
    }


    private static void withSpanAnnotationSpanDiscovery(Project project, @NotNull PsiFile psiFile, @NotNull DocumentInfo documentInfo) {
        PsiClass withSpanClass = JavaPsiFacade.getInstance(project).findClass(Constants.WITH_SPAN_FQN, GlobalSearchScope.allScope(project));
        //maybe the annotation is not in the classpath
        if (withSpanClass != null) {
            Query<PsiMethod> psiMethods = AnnotatedElementsSearch.searchPsiMethods(withSpanClass, GlobalSearchScope.fileScope(psiFile));
            psiMethods = filterNonRelevantMethodsForSpanDiscovery(psiMethods);
            psiMethods.forEach(psiMethod -> {
                List<SpanInfo> spanInfos = JavaSpanDiscoveryUtils.getSpanInfoFromWithSpanAnnotatedMethod(psiMethod);
                if (spanInfos != null) {
                    spanInfos.forEach(spanInfo -> {
                        Log.log(LOGGER::debug, "Found span info {} for method {}", spanInfo.getId(), spanInfo.getContainingMethodId());
                        MethodInfo methodInfo = documentInfo.getMethods().get(spanInfo.getContainingMethodId());
                        //this method must exist in the document info
                        Objects.requireNonNull(methodInfo, "method info " + spanInfo.getContainingMethodId() + " must exist in DocumentInfo for " + documentInfo.getFileUri());
                        methodInfo.getSpans().add(spanInfo);
                    });
                }
            });
        }
    }

}
