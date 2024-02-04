package org.digma.intellij.plugin.idea.psi.discovery.endpoint;

import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.util.Query;
import com.intellij.util.text.VersionComparatorUtil;
import org.digma.intellij.plugin.common.SearchScopeProvider;
import org.digma.intellij.plugin.idea.deps.ModulesDepsService;
import org.digma.intellij.plugin.idea.psi.java.*;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static org.digma.intellij.plugin.idea.psi.JvmCodeObjectsUtilsKt.createPsiMethodCodeObjectId;
import static org.digma.intellij.plugin.psi.PsiAccessUtilsKt.*;

public class MicronautFramework extends EndpointDiscovery {

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
    private PsiClass controllerAnnotationClass;
    private List<JavaAnnotation> httpMethodsAnnotations;

    public MicronautFramework(Project project) {
        this.project = project;
    }

    @NotNull
    @Override
    public String getName() {
        return "Micronaut";
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

    private boolean isMicronautHttpRelevant() {
        return controllerAnnotationClass != null;
    }

    @Override
    public List<EndpointInfo> lookForEndpoints(@NotNull SearchScopeProvider searchScopeProvider) {
        lateInit();
        if (!isMicronautHttpRelevant()) {
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
                    PsiClass controllerClass = currPsiMethod.getContainingClass();
                    if (controllerClass == null) {
                        return; // very unlikely
                    }
                    PsiAnnotation controllerAnnotation = controllerClass.getAnnotation(CONTROLLER_ANNOTATION_STR);
                    if (controllerAnnotation == null) {
                        return; // skip this method, since its class is not a controller
                    }
                    String endpointUriPrefix = JavaLanguageUtils.getPsiAnnotationAttributeValue(controllerAnnotation, "value");

                    String methodCodeObjectId = createPsiMethodCodeObjectId(currPsiMethod);
                    String httpEndpointCodeObjectId = createHttpEndpointCodeObjectId(currPsiMethod, currAnnotation, endpointUriPrefix);
                    if (httpEndpointCodeObjectId == null) {
                        return; // skip this method, since endpoint value could not be determined
                    }

                    EndpointInfo endpointInfo = new EndpointInfo(httpEndpointCodeObjectId, methodCodeObjectId, JavaPsiUtils.toFileUri(currPsiMethod), null, EndpointFramework.Micronaut);
                    Log.log(LOGGER::debug, "Found endpoint info '{}' for method '{}'", endpointInfo.getId(), endpointInfo.getContainingMethodId());

                    retList.add(endpointInfo);
                });

            }
        });
        return retList;
    }

    @Nullable
    private String createHttpEndpointCodeObjectId(PsiMethod psiMethod, JavaAnnotation httpMethodAnnotation, String endpointUriPrefix) {
        PsiAnnotation httpPsiAnnotation = psiMethod.getAnnotation(httpMethodAnnotation.getClassNameFqn());
        if (httpPsiAnnotation == null) {
            return null;
        }

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

        // value for example : 'epHTTP:HTTP GET - /books/get'
        var endpointId =
                // digma part
                "epHTTP:" + "HTTP " + httpMethodUcase + " ";
        // Micronaut part
        if (isMicronaut376OrBellow(psiMethod)) {
            //fix for micronaut 3.7.6 and bellow where the endpoint id should contain double GET GET
            endpointId = endpointId + httpMethodUcase + " - " + EndpointDiscoveryUtils.combineUri(endpointUriPrefix, endpointUriSuffix);
        } else {
            endpointId = endpointId + EndpointDiscoveryUtils.combineUri(endpointUriPrefix, endpointUriSuffix);
        }

        return endpointId;
    }

    private boolean isMicronaut376OrBellow(PsiMethod psiMethod) {

        var module = ModuleUtilCore.findModuleForPsiElement(psiMethod);
        if (module == null) {
            return false;
        }

        var moduleRootManager = ModuleRootManager.getInstance(module);
        var orderEntries = moduleRootManager.getOrderEntries();

        var micronautLib = Arrays.stream(orderEntries)
                .filter(orderEntry -> orderEntry instanceof LibraryOrderEntry &&
                        ((LibraryOrderEntry) orderEntry).getLibraryName() != null &&
                        ((LibraryOrderEntry) orderEntry).getLibraryName().contains("io.micronaut:micronaut-core"))
                .findFirst().orElse(null);

        if (micronautLib == null) {
            return false;
        }

        var micronautVersion = ModulesDepsService.toUnifiedCoordinates((LibraryOrderEntry) micronautLib).getVersion();
        return VersionComparatorUtil.compare("3.7.6", micronautVersion) >= 0;


        //todo:can use ModulesDepsService but it does not update if configuration changes, for example library added or version changed
        //        var module =  ModuleUtilCore.findModuleForPsiElement(psiMethod);
        //        if (module == null){
        //            return false;
        //        }
        //        var modulesDepsService = ModulesDepsService.getInstance(project);
        //        var moduleExt = modulesDepsService.getModuleExt(module.getName());
        //        if (moduleExt == null) {
        //            return false;
        //        }
        //
        //        var micronautVersion = moduleExt.getMetadata().getMicronautVersion();
        //        return VersionComparatorUtil.compare("3.7.6", micronautVersion) >= 0;

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
