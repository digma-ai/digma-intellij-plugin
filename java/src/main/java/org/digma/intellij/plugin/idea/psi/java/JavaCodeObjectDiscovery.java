package org.digma.intellij.plugin.idea.psi.java;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.PsiExtensibleClass;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.digma.intellij.plugin.idea.psi.java.Constants.SPAN_BUILDER_FQN;
import static org.digma.intellij.plugin.idea.psi.java.JavaLanguageUtils.createJavaMethodCodeObjectId;
import static org.digma.intellij.plugin.idea.psi.java.JavaSpanDiscoveryUtils.filterNonRelevantMethodsForSpanDiscovery;
import static org.digma.intellij.plugin.idea.psi.java.JavaSpanDiscoveryUtils.filterNonRelevantReferencesForSpanDiscovery;

@SuppressWarnings("UnstableApiUsage")
public class JavaCodeObjectDiscovery {

    private static final Logger LOGGER = Logger.getInstance(JavaCodeObjectDiscovery.class);


    public static @NotNull DocumentInfo buildDocumentInfo(@NotNull Project project, @NotNull PsiJavaFile psiJavaFile, @NotNull List<IEndpointDiscovery> endpointDiscoveryList) {
        var stopWatch = StopWatch.createStarted();

        try {
            DocumentInfo documentInfo = JavaDocumentInfoIndex.tryGetDocumentInfoFromIndex(project, psiJavaFile);
            if (documentInfo == null) {
                documentInfo = buildDocumentInfoImpl(project, psiJavaFile);
            }
            /*
            why do we need a separate enrichDocumentInfo stage?
            when building the DocumentInfo index there is no guarantee that reference resolving will work, it depends on the
            state of intellij index. so span discovery may not work during indexing.
            JavaDocumentInfoIndex does not even try to run span discovery, it only does method discovery.
            so when we take the DocumentInfo from the index we assume that it doesn't contain span discovery and endpoint discovery.
            enrichDocumentInfo will run span discovery and endpoint discovery and add it to the methods of DocumentInfo.
            buildDocumentInfo is always called in smart mode and so span discovery and endpoint discovery should work.

             */
            enrichDocumentInfo(project, documentInfo, psiJavaFile, endpointDiscoveryList);
            return documentInfo;

        } finally {
            stopWatch.stop();
            Log.log(LOGGER::debug, "buildDocumentInfo for {} took {} milliseconds", psiJavaFile.getName(), stopWatch.getTime(TimeUnit.MILLISECONDS));
        }
    }


    public static DocumentInfo buildDocumentInfoImpl(@NotNull Project project, @NotNull PsiJavaFile psiJavaFile) {

        String fileUri = PsiUtils.psiFileToUri(psiJavaFile);
        Map<String, MethodInfo> methodInfoMap = new HashMap<>();

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

            final List<PsiMethod> methods;
            if (aClass instanceof PsiExtensibleClass) {
                // avoid cases when there are generated methods and/or constructors such as lombok creates,
                // see issue https://github.com/digma-ai/digma-intellij-plugin/issues/833
                // see issue https://youtrack.jetbrains.com/issue/IDEA-323198
                PsiExtensibleClass extClass = (PsiExtensibleClass) aClass;
                methods = extClass.getOwnMethods();
            } else {
                // call to getMethods might cause issue https://github.com/digma-ai/digma-intellij-plugin/issues/833, so avoiding it if possible
                PsiMethod[] methodsArr = aClass.getMethods(); // call to getMethods might cause issue 833
                methods = List.of(methodsArr);
            }

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


    private static void enrichDocumentInfo(Project project, @NotNull DocumentInfo documentInfo, @NotNull PsiFile psiFile, @NotNull List<IEndpointDiscovery> endpointDiscoveryList) {
        /*
        need to make sure that spans and endpoints are cleared here.
        why?
        This DocumentInfo may have already been enriched before,when it was enriched it may already contain span and endpoints.
        todo: not sure its necessary to remove because it seems that the index returns a copy, so when adding span and endpoint discovery
         it is not saved to the index and the next time we take a DocumentInfo from the index it will not contain span and endpoint discovery.
         */
        documentInfo.getMethods().forEach((id, methodInfo) -> {
            methodInfo.getSpans().clear();
            methodInfo.getEndpoints().clear();
        });

        spanDiscovery(project, psiFile, documentInfo);
        endpointDiscovery(psiFile, documentInfo, endpointDiscoveryList);
    }

    private static void spanDiscovery(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull DocumentInfo documentInfo) {
        Log.log(LOGGER::debug, "Building spans for file {}", psiFile);
        withSpanAnnotationSpanDiscovery(project, psiFile, documentInfo);
        startSpanMethodCallSpanDiscovery(project, psiFile, documentInfo);
    }

    private static void endpointDiscovery(@NotNull PsiFile psiFile, @NotNull DocumentInfo documentInfo, @NotNull List<IEndpointDiscovery> endpointDiscoveryList) {
        Log.log(LOGGER::debug, "Building endpoints for file {}", psiFile);
        endpointDiscoveryList.forEach(it -> it.endpointDiscovery(psiFile, documentInfo));
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
