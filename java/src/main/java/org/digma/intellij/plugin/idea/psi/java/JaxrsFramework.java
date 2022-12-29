package org.digma.intellij.plugin.idea.psi.java;

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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class JaxrsFramework {

    private static final Logger LOGGER = Logger.getInstance(JaxrsFramework.class);
    private static final String JAX_RS_PATH_ANNOTATION_STR = "javax.ws.rs.Path";
    private static final String HTTP_DELETE_ANNOTATION_STR = "javax.ws.rs.DELETE";
    private static final String HTTP_GET_ANNOTATION_STR = "javax.ws.rs.GET";
    private static final String HTTP_HEAD_ANNOTATION_STR = "javax.ws.rs.HEAD";
    private static final String HTTP_OPTIONS_ANNOTATION_STR = "javax.ws.rs.OPTIONS";
    private static final String HTTP_PATCH_ANNOTATION_STR = "javax.ws.rs.PATCH";
    private static final String HTTP_POST_ANNOTATION_STR = "javax.ws.rs.POST";
    private static final String HTTP_PUT_ANNOTATION_STR = "javax.ws.rs.PUT";
    private static final List<String> HTTP_METHODS_ANNOTATION_STR_LIST = List.of(
            HTTP_DELETE_ANNOTATION_STR, HTTP_GET_ANNOTATION_STR, HTTP_HEAD_ANNOTATION_STR, HTTP_OPTIONS_ANNOTATION_STR,
            HTTP_PATCH_ANNOTATION_STR, HTTP_POST_ANNOTATION_STR, HTTP_PUT_ANNOTATION_STR);

    private final Project project;

    // late init
    private boolean lateInitAlready = false;
    private PsiClass jaxrsPathAnnotationClass;
    private List<JavaAnnotation> httpMethodsAnnotations;

    public JaxrsFramework(Project project) {
        this.project = project;
    }

    private void lateInit() {
        if (lateInitAlready) return;

        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
        jaxrsPathAnnotationClass = psiFacade.findClass(JAX_RS_PATH_ANNOTATION_STR, GlobalSearchScope.allScope(project));
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

    private boolean isJaxRsHttpRelevant() {
        return jaxrsPathAnnotationClass != null;
    }

    public void endpointDiscovery(@NotNull PsiFile psiFile, @NotNull DocumentInfo documentInfo) {
        lateInit();
        if (!isJaxRsHttpRelevant()) {
            return;
        }

        httpMethodsAnnotations.forEach(currAnnotation -> {
            Query<PsiMethod> psiMethodsOfHttpMethodInFile = AnnotatedElementsSearch.searchPsiMethods(currAnnotation.getPsiClass(), GlobalSearchScope.fileScope(psiFile));

            for (PsiMethod currPsiMethod : psiMethodsOfHttpMethodInFile) {
                PsiClass controllerClass = currPsiMethod.getContainingClass();
                if (controllerClass == null) {
                    continue; // very unlikely
                }
                PsiAnnotation controllerAnnotation = controllerClass.getAnnotation(JAX_RS_PATH_ANNOTATION_STR);
                if (controllerAnnotation == null) {
                    continue; // skip this method, since its class is not a controller
                }
                String endpointUriPrefix = JavaLanguageUtils.getPsiAnnotationAttributeValue(controllerAnnotation, "value");

                String endpointUriSuffix = "";
                PsiAnnotation methodPathAnnotation = currPsiMethod.getAnnotation(JAX_RS_PATH_ANNOTATION_STR); // optional annotation on method
                if (methodPathAnnotation != null) {
                    endpointUriSuffix = JavaLanguageUtils.getPsiAnnotationAttributeValue(methodPathAnnotation, "value");
                }

                String endpointFullUri = JavaUtils.combineUri(endpointUriPrefix, endpointUriSuffix);

                String httpEndpointCodeObjectId = createHttpEndpointCodeObjectId(currAnnotation, endpointFullUri);

                EndpointInfo endpointInfo = new EndpointInfo(httpEndpointCodeObjectId, JavaLanguageUtils.createJavaMethodCodeObjectId(currPsiMethod), documentInfo.getFileUri());
                Log.log(LOGGER::debug, "Found endpoint info '{}' for method '{}'", endpointInfo.getId(), endpointInfo.getContainingMethodId());

                MethodInfo methodInfo = documentInfo.getMethods().get(endpointInfo.getContainingMethodId());
                //this method must exist in the document info
                Objects.requireNonNull(methodInfo, "method info " + endpointInfo.getContainingMethodId() + " must exist in DocumentInfo for " + documentInfo.getFileUri());
                methodInfo.addEndpoint(endpointInfo);
            }
        });
    }

    @NotNull
    protected static String createHttpEndpointCodeObjectId(JavaAnnotation httpMethodAnnotation, String endpointFullUri) {
        String httpMethodUcase = getHttpMethod(httpMethodAnnotation).toUpperCase();

        // value for example : 'epHTTP:HTTP GET /books/get'
        return "" +
                // digma part
                "epHTTP:" + "HTTP " + httpMethodUcase + " " +
                // JaxRs part
                endpointFullUri
                ;
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
