package org.digma.intellij.plugin.idea.psi.java;

import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.util.Query;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.EndpointInfo;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MicronautFramework implements IEndpointDiscovery {

    private static final Logger LOGGER = Logger.getInstance(MicronautFramework.class);
    private static final String CONTROLLER_ANNOTATION_STR = "io.micronaut.http.annotation.Controller";
    private static final String HTTP_DELETE_ANNOTATION_STR = "io.micronaut.http.annotation.Delete";
    private static final String HTTP_GET_ANNOTATION_STR = "io.micronaut.http.annotation.Get";
    private static final String HTTP_HEAD_ANNOTATION_STR = "io.micronaut.http.annotation.Head";
    private static final String HTTP_OPTIONS_ANNOTATION_STR = "io.micronaut.http.annotation.Options";
    private static final String HTTP_PATCH_ANNOTATION_STR = "io.micronaut.http.annotation.Patch";
    private static final String HTTP_POST_ANNOTATION_STR = "io.micronaut.http.annotation.Post";
    private static final String HTTP_PUT_ANNOTATION_STR = "io.micronaut.http.annotation.Put";
    private static final String HTTP_TRACE_ANNOTATION_STR = "io.micronaut.http.annotation.Trace";
    private static final List<String> HTTP_METHODS_ANNOTATION_STR_LIST = List.of(
            HTTP_DELETE_ANNOTATION_STR, HTTP_GET_ANNOTATION_STR, HTTP_HEAD_ANNOTATION_STR, HTTP_OPTIONS_ANNOTATION_STR,
            HTTP_PATCH_ANNOTATION_STR, HTTP_POST_ANNOTATION_STR, HTTP_PUT_ANNOTATION_STR, HTTP_TRACE_ANNOTATION_STR);

    private static final List<String> VALUE_OR_URI = List.of("value", "uri");

    private final Project project;

    // late init
    private boolean lateInitAlready = false;
    private PsiClass controllerAnnotationClass;
    private List<JavaAnnotation> httpMethodsAnnotations;

    public MicronautFramework(Project project) {
        this.project = project;
    }

    private void lateInit() {
        if (lateInitAlready) return;

        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
        controllerAnnotationClass = psiFacade.findClass(CONTROLLER_ANNOTATION_STR, GlobalSearchScope.allScope(project));
        initHttpMethodAnnotations(psiFacade);

        lateInitAlready = true;
    }

    private void initHttpMethodAnnotations(JavaPsiFacade psiFacade) {
        httpMethodsAnnotations = HTTP_METHODS_ANNOTATION_STR_LIST.stream()
                .map(currFqn -> {
                    PsiClass psiClass = psiFacade.findClass(currFqn, GlobalSearchScope.allScope(project));
                    if (psiClass == null) return null;
                    return new JavaAnnotation(currFqn, psiClass);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableList());
    }

    private boolean isMicronautHttpRelevant() {
        return controllerAnnotationClass != null;
    }

    public void endpointDiscovery(@NotNull PsiFile psiFile, @NotNull DocumentInfo documentInfo) {
        lateInit();
        if (!isMicronautHttpRelevant()) {
            return;
        }

        httpMethodsAnnotations.forEach(currAnnotation -> {
            Query<PsiMethod> psiMethodsInFile = AnnotatedElementsSearch.searchPsiMethods(currAnnotation.getPsiClass(), GlobalSearchScope.fileScope(psiFile));

            for (PsiMethod currPsiMethod : psiMethodsInFile) {
                PsiClass controllerClass = currPsiMethod.getContainingClass();
                if (controllerClass == null) {
                    continue; // very unlikely
                }
                PsiAnnotation controllerAnnotation = controllerClass.getAnnotation(CONTROLLER_ANNOTATION_STR);
                if (controllerAnnotation == null) {
                    continue; // skip this method, since its class is not a controller
                }
                String endpointUriPrefix = JavaLanguageUtils.getPsiAnnotationAttributeValue(controllerAnnotation, "value");

                String methodCodeObjectId = JavaLanguageUtils.createJavaMethodCodeObjectId(currPsiMethod);
                String httpEndpointCodeObjectId = createHttpEndpointCodeObjectId(currPsiMethod, currAnnotation, endpointUriPrefix);
                if (httpEndpointCodeObjectId == null) {
                    continue; // skip this method, since endpoint value could not be determined
                }

                EndpointInfo endpointInfo = new EndpointInfo(httpEndpointCodeObjectId, methodCodeObjectId, documentInfo.getFileUri());
                Log.log(LOGGER::debug, "Found endpoint info '{}' for method '{}'", endpointInfo.getId(), endpointInfo.getContainingMethodId());

                MethodInfo methodInfo = documentInfo.getMethods().get(endpointInfo.getContainingMethodId());
                //this method must exist in the document info
                Objects.requireNonNull(methodInfo, "method info " + endpointInfo.getContainingMethodId() + " must exist in DocumentInfo for " + documentInfo.getFileUri());
                methodInfo.addEndpoint(endpointInfo);
            }
        });
    }

    @Nullable
    protected static String createHttpEndpointCodeObjectId(PsiMethod psiMethod, JavaAnnotation httpMethodAnnotation, String endpointUriPrefix) {
        PsiAnnotation httpPsiAnnotation = psiMethod.getAnnotation(httpMethodAnnotation.getClassNameFqn());
        List<JvmAnnotationAttribute> annotationAttributes = httpPsiAnnotation.getAttributes();

        String endpointUriSuffix = "/";
        for (JvmAnnotationAttribute curr : annotationAttributes) {
            // taking the first attribute, either "value" or "uri" - that's how micronaut behave if both exists
            if (VALUE_OR_URI.contains(curr.getAttributeName())) {
                endpointUriSuffix = JavaLanguageUtils.getPsiAnnotationAttributeValue(httpPsiAnnotation, curr.getAttributeName());
                if (endpointUriSuffix == null) {
                    Log.log(LOGGER::debug, "cannot create http endpoint for method '{}' since could not extract attribute value for name '{}' from annotation '{}'",
                            psiMethod.getName(), curr.getAttributeName(), httpMethodAnnotation.getClassNameFqn());
                    return null; // unlikely
                }
                break; // found the first occurrence, and out
            }
            // note: attribute of "uris" is irrelevant
        }

        String httpMethodUcase = getHttpMethod(httpMethodAnnotation).toUpperCase();

        // value for example : 'epHTTP:HTTP GET GET - /books/get'
        return "" +
                // digma part
                "epHTTP:" + "HTTP " + httpMethodUcase + " " +
                // Micronaut part
                httpMethodUcase + " - " + JavaUtils.combineUri(endpointUriPrefix, endpointUriSuffix);
    }

    @NotNull
    private static String getHttpMethod(JavaAnnotation javaAnnotation) {
        String fqn = javaAnnotation.getClassNameFqn();
        int lastIndexOfDot = fqn.lastIndexOf('.');
        if (lastIndexOfDot >= 0) {
            return fqn.substring(lastIndexOfDot + 1);
        } else {
            return fqn;
        }
    }
}
