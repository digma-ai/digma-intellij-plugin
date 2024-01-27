package org.digma.intellij.plugin.idea.psi.discovery.endpoint;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.search.searches.OverridingMethodsSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Query;
import org.digma.intellij.plugin.idea.psi.java.JavaLanguageUtils;
import org.digma.intellij.plugin.idea.psi.java.JavaPsiUtils;
import org.digma.intellij.plugin.model.discovery.EndpointFramework;
import org.digma.intellij.plugin.model.discovery.EndpointInfo;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import static org.digma.intellij.plugin.idea.psi.JvmCodeObjectsUtilsKt.createPsiMethodCodeObjectId;
import static org.digma.intellij.plugin.idea.psi.PsiAccessUtilsKt.runInReadAccessInSmartModeAndRetry;
import static org.digma.intellij.plugin.idea.psi.PsiAccessUtilsKt.runInReadAccessInSmartModeWithResultAndRetry;

public abstract class AbstractJaxrsFramework extends EndpointDiscovery {

    private static final Logger LOGGER = Logger.getInstance(AbstractJaxrsFramework.class);

    abstract String getJaxRsPackageName();

    protected abstract EndpointFramework getFramework();

    private final List<String> HTTP_METHODS_ANNOTATION_STR_LIST;

    private final Project project;

    // late init
    private PsiClass jaxrsPathAnnotationClass;
    private List<JavaAnnotation> httpMethodsAnnotations;

    public AbstractJaxrsFramework(Project project) {
        this.project = project;

        this.HTTP_METHODS_ANNOTATION_STR_LIST = List.of(
                HTTP_DELETE_ANNOTATION_STR(), HTTP_GET_ANNOTATION_STR(), HTTP_HEAD_ANNOTATION_STR(), HTTP_OPTIONS_ANNOTATION_STR(),
                HTTP_PATCH_ANNOTATION_STR(), HTTP_POST_ANNOTATION_STR(), HTTP_PUT_ANNOTATION_STR());
    }

    protected String JAX_RS_PATH_ANNOTATION_STR() {
        // "jakarta.ws.rs.Path" or "javax.ws.rs.Path"
        return getJaxRsPackageName() + ".Path";
    }

    protected String HTTP_DELETE_ANNOTATION_STR() {
        return getJaxRsPackageName() + ".DELETE";
    }

    protected String HTTP_GET_ANNOTATION_STR() {
        return getJaxRsPackageName() + ".GET";
    }

    protected String HTTP_HEAD_ANNOTATION_STR() {
        return getJaxRsPackageName() + ".HEAD";
    }

    protected String HTTP_OPTIONS_ANNOTATION_STR() {
        return getJaxRsPackageName() + ".OPTIONS";
    }

    protected String HTTP_PATCH_ANNOTATION_STR() {
        return getJaxRsPackageName() + ".PATCH";
    }

    protected String HTTP_POST_ANNOTATION_STR() {
        return getJaxRsPackageName() + ".POST";
    }

    protected String HTTP_PUT_ANNOTATION_STR() {
        return getJaxRsPackageName() + ".PUT";
    }

    protected String getApplicationPathAnnotationClassFqn() {
        return getJaxRsPackageName() + ".ApplicationPath";
    }

    //todo:maybe synchronize because may be called from multiple threads
    private void lateInit() {

        runInReadAccessInSmartModeAndRetry(project, () -> {
            JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
            jaxrsPathAnnotationClass = psiFacade.findClass(JAX_RS_PATH_ANNOTATION_STR(), GlobalSearchScope.allScope(project));
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

    private boolean isJaxRsHttpRelevant() {
        return jaxrsPathAnnotationClass != null;
    }

    /**
     * Overriding since have different logic,
     * in this impl searching for classes/methods in file and see if they inherit from super classes which declare on @PATH annotations.
     */
    @Override
    public List<EndpointInfo> lookForEndpoints(@NotNull PsiFile psiFile) {
        lateInit();
        if (!isJaxRsHttpRelevant()) {
            return Collections.emptyList();
        }

        List<EndpointInfo> endpointInfos = new ArrayList<>();

        Collection<PsiClass> allClassesInFile =
                runInReadAccessInSmartModeWithResultAndRetry(project, () ->
                        PsiTreeUtil.findChildrenOfType(psiFile, PsiClass.class));


        for (final PsiClass currClass : allClassesInFile) {

            List<PsiMethod> methodsInClass = JavaPsiUtils.getMethodsOf(project, currClass);

            for (final PsiMethod currPsiMethod : methodsInClass) {


                runInReadAccessInSmartModeAndRetry(project, () -> {
                    Set<PsiMethod> candidateMethods = new HashSet<>();
                    final PsiAnnotation methodPathAnnotation = JavaPsiUtils.findNearestAnnotation(currPsiMethod, JAX_RS_PATH_ANNOTATION_STR());
                    boolean hasPath = methodPathAnnotation != null;
                    boolean hasHttpMethod = httpMethodsAnnotations.stream()
                            .anyMatch(it -> JavaPsiUtils.findNearestAnnotation(currPsiMethod, it.getClassNameFqn()) != null);

                    if (hasPath || hasHttpMethod) {
                        candidateMethods.add(currPsiMethod);
                    }
                    endpointInfos.addAll(handleCandidateMethods(candidateMethods).stream().toList());
                });
            }
        }

        return endpointInfos;
    }

    @Override
    public List<EndpointInfo> lookForEndpoints(@NotNull Supplier<SearchScope> searchScopeSupplier) {
        lateInit();
        if (!isJaxRsHttpRelevant()) {
            return Collections.emptyList();
        }

        List<EndpointInfo> endpointInfos = new ArrayList<>();



        for (JavaAnnotation currExpectedAnnotation : httpMethodsAnnotations) {

            Collection<PsiMethod> methodsWithDirectHttpMethod =
                    runInReadAccessInSmartModeWithResultAndRetry(project, () -> {
                Query<PsiMethod> psiMethods = AnnotatedElementsSearch.searchPsiMethods(currExpectedAnnotation.getPsiClass(), searchScopeSupplier.get());
                return psiMethods.findAll();
                    });


            for (final PsiMethod directMethodWithHttpMethod : methodsWithDirectHttpMethod) {

                runInReadAccessInSmartModeAndRetry(project, () -> {
                    Set<PsiMethod> candidateMethods = new HashSet<>();
                    candidateMethods.add(directMethodWithHttpMethod);
                    Query<PsiMethod> overridingMethods = OverridingMethodsSearch.search(directMethodWithHttpMethod);
                    candidateMethods.addAll(overridingMethods.findAll());
                    endpointInfos.addAll(handleCandidateMethods(candidateMethods).stream().toList());
                });

            }
        }

        return endpointInfos;
    }

    protected Set<EndpointInfo> handleCandidateMethods(Collection<PsiMethod> candidateMethods) {
        Set<EndpointInfo> retSet = new HashSet<>();

        for (final PsiMethod currPsiMethod : candidateMethods) {
            final PsiAnnotation methodPathAnnotation = JavaPsiUtils.findNearestAnnotation(currPsiMethod, JAX_RS_PATH_ANNOTATION_STR());

            final PsiClass currClass = currPsiMethod.getContainingClass();
            final PsiAnnotation controllerPathAnnotation = currClass == null ? null : JavaPsiUtils.findNearestAnnotation(currClass, JAX_RS_PATH_ANNOTATION_STR());

            if (methodPathAnnotation == null && controllerPathAnnotation == null) {
                continue; // skip since could not find annotation of @Path, in either class and or method
            }
            final Set<String> appPaths = evaluateApplicationPaths(currPsiMethod);

            for (JavaAnnotation currExpectedAnnotation : httpMethodsAnnotations) {
                PsiAnnotation httpMethodAnnotation = JavaPsiUtils.findNearestAnnotation(currPsiMethod, currExpectedAnnotation.getClassNameFqn());
                if (httpMethodAnnotation == null) {
                    continue; // skipping since could not find annotation of HTTP Method, such as @GET
                }
                String endpointSuffixUri = combinePaths(controllerPathAnnotation, methodPathAnnotation);

                for (String appPath : appPaths) {
                    String endpointFullUri = EndpointDiscoveryUtils.combineUri(appPath, endpointSuffixUri);
                    String httpEndpointCodeObjectId = createHttpEndpointCodeObjectId(currExpectedAnnotation, endpointFullUri);

                    EndpointInfo endpointInfo = new EndpointInfo(httpEndpointCodeObjectId, createPsiMethodCodeObjectId(currPsiMethod), JavaPsiUtils.toFileUri(currPsiMethod), null, getFramework());
                    retSet.add(endpointInfo);
                }
            }
        }

        return retSet;
    }

    protected Set<String> evaluateApplicationPaths(@NotNull PsiElement psiElement) {
        var psiFacade = JavaPsiFacade.getInstance(project);
        final PsiClass appPathAnnotationClass = psiFacade.findClass(getApplicationPathAnnotationClassFqn(), GlobalSearchScope.allScope(project));

        Set<String> appPaths = new HashSet<>();

        // check for ApplicationPath in context of module
        if (appPathAnnotationClass != null) {
            Module module = ModuleUtilCore.findModuleForPsiElement(psiElement);
            if (module != null) {
                Query<PsiClass> appPathPsiClasses = AnnotatedElementsSearch.searchPsiClasses(appPathAnnotationClass, GlobalSearchScope.moduleScope(module));
                for (PsiClass appPathClass : appPathPsiClasses) {
                    PsiAnnotation appPathAnnotation = appPathClass.getAnnotation(getApplicationPathAnnotationClassFqn());
                    if (appPathAnnotation == null) {
                        // very unlikely, maybe need to log an error
                        continue;
                    }
                    String appPathValue = JavaLanguageUtils.getPsiAnnotationAttributeValue(appPathAnnotation, "value");
                    appPaths.add(appPathValue);
                }
            }
        }

        if (appPaths.isEmpty()) {
            appPaths.add("/"); // make sure have at least one entry
        }

        return appPaths;
    }

    protected static String combinePaths(PsiAnnotation annotOfPrefix, PsiAnnotation annotOfSuffix) {
        String prefixStr = "";
        if (annotOfPrefix != null) {
            prefixStr = JavaLanguageUtils.getPsiAnnotationAttributeValue(annotOfPrefix, "value");
        }
        String suffixStr = "";
        if (annotOfSuffix != null) {
            suffixStr = JavaLanguageUtils.getPsiAnnotationAttributeValue(annotOfSuffix, "value");
        }
        return EndpointDiscoveryUtils.combineUri(prefixStr, suffixStr);
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
