package org.digma.intellij.plugin.idea.psi.discovery.endpoint;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.util.Query;
import org.digma.intellij.plugin.common.SearchScopeProvider;
import org.digma.intellij.plugin.idea.psi.java.*;
import org.digma.intellij.plugin.model.discovery.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static org.digma.intellij.plugin.idea.psi.JvmCodeObjectsUtilsKt.createPsiMethodCodeObjectId;
import static org.digma.intellij.plugin.psi.PsiAccessUtilsKt.*;

public class SpringBootFramework extends EndpointDiscovery {

    private static final Logger LOGGER = Logger.getInstance(SpringBootFramework.class);
    private static final String CONTROLLER_ANNOTATION_STR = "org.springframework.stereotype.Controller";
    private static final String REST_CONTROLLER_ANNOTATION_STR = "org.springframework.web.bind.annotation.RestController";
    private static final String HTTP_DELETE_ANNOTATION_STR = "org.springframework.web.bind.annotation.DeleteMapping";
    private static final String HTTP_GET_ANNOTATION_STR = "org.springframework.web.bind.annotation.GetMapping";
    private static final String HTTP_PATCH_ANNOTATION_STR = "org.springframework.web.bind.annotation.PatchMapping";
    private static final String HTTP_POST_ANNOTATION_STR = "org.springframework.web.bind.annotation.PostMapping";
    private static final String HTTP_PUT_ANNOTATION_STR = "org.springframework.web.bind.annotation.PutMapping";
    private static final String HTTP_REQUEST_MAPPING_ANNOTATION_STR = "org.springframework.web.bind.annotation.RequestMapping";
    private static final List<String> HTTP_METHODS_ANNOTATION_STR_LIST = List.of(
            HTTP_DELETE_ANNOTATION_STR, HTTP_GET_ANNOTATION_STR, HTTP_PATCH_ANNOTATION_STR, HTTP_POST_ANNOTATION_STR, HTTP_PUT_ANNOTATION_STR, HTTP_REQUEST_MAPPING_ANNOTATION_STR);

    // related to RequestMapping, one of those attributes should be filled
    private static final List<String> ATTRIBUTES_OF_PATH = List.of("value", "path");
    //
    private static final Map<String, String> MAP_HTTP_XXX_ANNOT_2_HTTP_METHOD;

    static {
        MAP_HTTP_XXX_ANNOT_2_HTTP_METHOD = Map.of(
                HTTP_DELETE_ANNOTATION_STR, "DELETE",
                HTTP_GET_ANNOTATION_STR, "GET",
                HTTP_PATCH_ANNOTATION_STR, "PATCH",
                HTTP_POST_ANNOTATION_STR, "POST",
                HTTP_PUT_ANNOTATION_STR, "PUT"
        );
    }

    private final Project project;

    // late init
    private PsiClass controllerAnnotationClass;
    private List<JavaAnnotation> httpMethodsAnnotations;

    public SpringBootFramework(Project project) {
        this.project = project;
    }


    @NotNull
    @Override
    public String getName() {
        return "SpringBoot";
    }

    private void lateInit() {


        runInReadAccessInSmartModeAndRetry(project, () -> {
            JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
            controllerAnnotationClass = psiFacade.findClass(CONTROLLER_ANNOTATION_STR, GlobalSearchScope.allScope(project));
            initHttpMethodAnnotations(psiFacade);
        });
    }

    private void initHttpMethodAnnotations(JavaPsiFacade psiFacade) {
        httpMethodsAnnotations = HTTP_METHODS_ANNOTATION_STR_LIST.stream()
                .map(currFqn -> {
                    PsiClass psiClass = psiFacade.findClass(currFqn, GlobalSearchScope.allScope(project));
                    if (psiClass == null) return null;
                    return new JavaAnnotation(currFqn, psiClass);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private boolean isSpringBootWebRelevant() {
        return controllerAnnotationClass != null;
    }

    @Override
    public List<EndpointInfo> lookForEndpoints(@NotNull SearchScopeProvider searchScopeProvider) {
        lateInit();
        if (!isSpringBootWebRelevant()) {
            return Collections.emptyList();
        }

        List<EndpointInfo> retList = new ArrayList<>();

        httpMethodsAnnotations.forEach(currAnnotation -> {

            Collection<PsiMethod> psiMethodsInFile =
                    runInReadAccessInSmartModeWithResultAndRetry(project, () -> {
                        Query<PsiMethod> psiMethods = AnnotatedElementsSearch.searchPsiMethods(currAnnotation.getPsiClass(), searchScopeProvider.get());
                return psiMethods.findAll();
                    });


            for (PsiMethod currPsiMethod : psiMethodsInFile) {


                runInReadAccessInSmartModeAndRetry(project, () -> {
                    final String methodId = createPsiMethodCodeObjectId(currPsiMethod);
                    final PsiAnnotation mappingPsiAnnotationOnMethod = currPsiMethod.getAnnotation(currAnnotation.getClassNameFqn());
                    if (mappingPsiAnnotationOnMethod == null) {
                        return; // very unlikely
                    }

                    final PsiClass controllerClass = currPsiMethod.getContainingClass();
                    if (controllerClass == null) {
                        return; // very unlikely
                    }

                    if (!JavaPsiUtils.hasOneOfAnnotations(controllerClass, CONTROLLER_ANNOTATION_STR, REST_CONTROLLER_ANNOTATION_STR)) {
                        return; // skip this method, since its class is not a controller (or rest controller)
                    }

                    final PsiAnnotation controllerReqMappingAnnotation = controllerClass.getAnnotation(HTTP_REQUEST_MAPPING_ANNOTATION_STR);
                    String endpointUriPrefix = "";
                    if (controllerReqMappingAnnotation != null) {
                        endpointUriPrefix = JavaLanguageUtils.getValueOfFirstMatchingAnnotationAttribute(controllerReqMappingAnnotation, ATTRIBUTES_OF_PATH, "");
                    }

                    final String httpMethodName = evalHttpMethod(mappingPsiAnnotationOnMethod, controllerReqMappingAnnotation);
                    if (httpMethodName == null) {
                        return; // not likely
                    }

                    final List<String> endpointUriSuffixes = JavaLanguageUtils.getValuesOfFirstMatchingAnnotationAttribute(mappingPsiAnnotationOnMethod, ATTRIBUTES_OF_PATH);

                    for (String currSuffix : endpointUriSuffixes) {
                        String httpEndpointCodeObjectId = createHttpEndpointCodeObjectId(httpMethodName, endpointUriPrefix, currSuffix);
                        EndpointInfo endpointInfo = new EndpointInfo(httpEndpointCodeObjectId, methodId, JavaPsiUtils.toFileUri(currPsiMethod), null, EndpointFramework.SpringBoot);
                        retList.add(endpointInfo);
                    }
                });
            }
        });
        return retList;
    }

    @Nullable
    private String evalHttpMethod(@NotNull PsiAnnotation mappingPsiAnnotationOnMethod, @Nullable PsiAnnotation controllerReqMappingAnnotation) {

        String mappedValue = MAP_HTTP_XXX_ANNOT_2_HTTP_METHOD.get(mappingPsiAnnotationOnMethod.getQualifiedName());
        if (mappedValue != null) {
            return mappedValue;
        }

        if (HTTP_REQUEST_MAPPING_ANNOTATION_STR.equals(mappingPsiAnnotationOnMethod.getQualifiedName())) {
            // trying for attribute "method" in on method annotation
            {
                String value = JavaLanguageUtils.getPsiAnnotationAttributeValue(mappingPsiAnnotationOnMethod, "method");
                if (value != null) {
                    return value.toUpperCase();
                }
            }
            if (controllerReqMappingAnnotation != null) {
                // fallback to attribute "method" in on controller annotation
                String value = JavaLanguageUtils.getPsiAnnotationAttributeValue(controllerReqMappingAnnotation, "method");
                if (value != null) {
                    return value.toUpperCase();
                }
            }
        }

        return null;
    }

    @NotNull
    protected static String createHttpEndpointCodeObjectId(@NotNull String httpMethod, @Nullable String endpointUriPrefix, @Nullable String endpointUriSuffix) {
        // value for example : 'epHTTP:HTTP GET /books/get'
        return "" +
                // digma part
                "epHTTP:" + "HTTP " + httpMethod.toUpperCase() + " " +
                // Spring Web part
                EndpointDiscoveryUtils.combineUri(endpointUriPrefix, endpointUriSuffix);
    }

}
