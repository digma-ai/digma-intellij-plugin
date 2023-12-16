package org.digma.intellij.plugin.idea.psi.discovery.endpoint;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.util.Query;
import org.digma.intellij.plugin.common.Retries;
import org.digma.intellij.plugin.idea.psi.java.JavaLanguageUtils;
import org.digma.intellij.plugin.idea.psi.java.JavaPsiUtils;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.EndpointInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static org.digma.intellij.plugin.idea.psi.JvmCodeObjectsUtilsKt.createPsiMethodCodeObjectId;

public class GrpcFramework extends EndpointDiscovery {
    private static final Logger LOGGER = Logger.getInstance(GrpcFramework.class);

    public static final String BINDABLE_SERVICE_ANNOTATION_STR = "io.grpc.BindableService";
    public static final String DIGMA_UNKNOWN_SERVICE_NAME = "Digma.Unknown.Grpc.Service";

    private final Project project;

    // late init
    private boolean lateInitAlready = false;
    private PsiClass bindableServiceAnnotationClass;

    public GrpcFramework(Project project) {
        this.project = project;
    }

    private void lateInit() {

        Retries.simpleRetry(() -> JavaPsiUtils.runInReadAccess(project, () -> {
            JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
            bindableServiceAnnotationClass = psiFacade.findClass(BINDABLE_SERVICE_ANNOTATION_STR, GlobalSearchScope.allScope(project));
            Log.log(LOGGER::info, "GRPC init. isGrpcServerRelevant='{}'", isGrpcServerRelevant());
        }), Throwable.class, 50, 5);
    }

    private boolean isGrpcServerRelevant() {
        return bindableServiceAnnotationClass != null;
    }

    @Override
    @Nullable
    public List<EndpointInfo> lookForEndpoints(@NotNull Supplier<SearchScope> searchScopeSupplier) {
        lateInit();
        if (!isGrpcServerRelevant()) {
            return Collections.emptyList();
        }

        List<EndpointInfo> retList = new ArrayList<>();

        Collection<PsiClass> grpcServerClassesInFile = Retries.retryWithResult(() -> JavaPsiUtils.runInReadAccessWithResult(project, () -> {
            Query<PsiClass> psiClasses = ClassInheritorsSearch.search(bindableServiceAnnotationClass, searchScopeSupplier.get(), true);
            return psiClasses.findAll();
        }), Throwable.class, 50, 5);

        for (PsiClass currGrpcServerClass : grpcServerClassesInFile) {

            Retries.simpleRetry(() -> JavaPsiUtils.runInReadAccess(project, () -> {
                if (JavaPsiUtils.isBaseClass(currGrpcServerClass)) {
                    // if has no super class then it is the generated GRPC server class, we do not want it
                    Log.log(LOGGER::debug, "endpointDiscovery, skip bindableService GrpcServerClass fqn='{}' since it is the generated GRPC base service", currGrpcServerClass.getQualifiedName());
                    return;
                }
                Log.log(LOGGER::debug, "endpointDiscovery, bingo - its a GRPC server class, its fqn='{}'", currGrpcServerClass.getQualifiedName());
                List<EndpointInfo> endpointsAtClass = addEndpointMethods(currGrpcServerClass);
                retList.addAll(endpointsAtClass);
            }), Throwable.class, 50, 5);
        }
        return retList;
    }

    protected List<EndpointInfo> addEndpointMethods(@NotNull PsiClass grpcServerClass) {
        String grpcServiceName = evaluateServiceName(grpcServerClass);
        Log.log(LOGGER::debug, "addEndpointMethods for grpcServerClass fqn='{}' with evaluated serviceName='{}'", grpcServerClass.getQualifiedName(), grpcServiceName);

        List<EndpointInfo> retList = new ArrayList<>(16);
        Collection<PsiMethod> psiMethods = JavaPsiUtils.getMethodsOf(project, grpcServerClass);
        for (PsiMethod currPsiMethod : psiMethods) {
            String methodCodeObjectId = createPsiMethodCodeObjectId(currPsiMethod);

            String endpointId = createEndpointId(grpcServiceName, currPsiMethod);
            //PsiParameterList parameterList = currPsiMethod.getParameterList(); //TODO: maybe search for parameters of type io.grpc.stub.StreamObserver
            EndpointInfo endpointInfo = new EndpointInfo(endpointId, methodCodeObjectId, JavaPsiUtils.toFileUri(currPsiMethod), currPsiMethod.getTextOffset());
            retList.add(endpointInfo);
        }
        return retList;
    }

    @NotNull
    protected String evaluateServiceName(@NotNull PsiClass grpcServerClass) {
        PsiClass generatedGrpcBasePsiClass = JavaPsiUtils.climbUpToBaseClass(grpcServerClass); // for example GreeterGrpc.GreeterImplBase
        PsiClass generatedGrpcContainingPsiClass = generatedGrpcBasePsiClass.getContainingClass(); // for example GreeterGrpc
        if (generatedGrpcContainingPsiClass == null) {
            Log.log(LOGGER::warn, "evaluateServiceName:#PotentialBug: could not find containing (generated) class for generated GRPC ImplBase Class fqn='{}'", generatedGrpcBasePsiClass.getQualifiedName());
            return DIGMA_UNKNOWN_SERVICE_NAME; // very unlikely
        }

        // searching for generated field named SERVICE_NAME, for example :
        // public static final String SERVICE_NAME = "helloworld.Greeter"
        PsiField serviceNamePsiField = generatedGrpcContainingPsiClass.findFieldByName("SERVICE_NAME", false);
        if (serviceNamePsiField == null) {
            Log.log(LOGGER::warn, "evaluateServiceName:#PotentialBug: could not find field 'SERVICE_NAME' in containing (generated) class of GRPC fqn='{}'", generatedGrpcContainingPsiClass.getQualifiedName());
            return DIGMA_UNKNOWN_SERVICE_NAME; // very unlikely
        }

        Object serviceNameValueAsObj = serviceNamePsiField.computeConstantValue();
        if (serviceNameValueAsObj == null) {
            Log.log(LOGGER::warn, "evaluateServiceName:#PotentialBug: could not evaluate value of field 'SERVICE_NAME' in containing (generated) class of GRPC fqn='{}', with field={}", generatedGrpcContainingPsiClass.getQualifiedName(), serviceNamePsiField);
            return DIGMA_UNKNOWN_SERVICE_NAME; // very unlikely
        }

        return serviceNameValueAsObj.toString();
    }

    @NotNull
    protected String createEndpointId(@NotNull String grpcServiceName, @NotNull PsiMethod psiMethod) {
        //value for example: "epRPC:helloworld.Greeter/SayHello"
        return "" +
                // digma part
                "epRPC:" +
                // GRPC part
                grpcServiceName + "/" + capitalizeFirstLetter(psiMethod.getName());
    }

    @NotNull
    private static String capitalizeFirstLetter(@NotNull String value) {
        char firstCharUcase = Character.toUpperCase(value.charAt(0));
        return firstCharUcase + value.substring(1);
    }

}