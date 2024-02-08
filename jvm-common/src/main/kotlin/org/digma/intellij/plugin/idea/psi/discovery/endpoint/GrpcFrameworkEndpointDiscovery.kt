package org.digma.intellij.plugin.idea.psi.discovery.endpoint

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.search.searches.ClassInheritorsSearch
import org.digma.intellij.plugin.common.SearchScopeProvider
import org.digma.intellij.plugin.common.executeCatchingWithRetryIgnorePCE
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.common.runInReadAccessInSmartModeWithResultAndRetryIgnorePCE
import org.digma.intellij.plugin.common.runInReadAccessWithResult
import org.digma.intellij.plugin.common.runInReadAccessWithRetryIgnorePCE
import org.digma.intellij.plugin.idea.psi.PsiPointers
import org.digma.intellij.plugin.idea.psi.climbUpToBaseClass
import org.digma.intellij.plugin.idea.psi.createPsiMethodCodeObjectId
import org.digma.intellij.plugin.idea.psi.getMethodsInClass
import org.digma.intellij.plugin.idea.psi.isBaseClass
import org.digma.intellij.plugin.idea.psi.toFileUri
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.EndpointFramework
import org.digma.intellij.plugin.model.discovery.EndpointInfo
import org.digma.intellij.plugin.progress.ProcessContext

private const val BINDABLE_SERVICE_ANNOTATION_STR = "io.grpc.BindableService"
private const val DIGMA_UNKNOWN_SERVICE_NAME = "Digma.Unknown.Grpc.Service"


open class GrpcFrameworkEndpointDiscovery(private val project: Project) : EndpointDiscovery() {

    private val logger = Logger.getInstance(this::class.java)

    override fun getName(): String {
        return "Grpc"
    }

    private fun getBindableServiceAnnotationClass(): PsiClass? {
        return project.service<PsiPointers>().getPsiClass(project, BINDABLE_SERVICE_ANNOTATION_STR)
    }

    private fun isGrpcServerRelevant(): Boolean {
        return getBindableServiceAnnotationClass() != null
    }


    override fun lookForEndpoints(searchScopeProvider: SearchScopeProvider, context: ProcessContext): List<EndpointInfo>? {

        if (!isGrpcServerRelevant() || !isProjectValid(project)) {
            return emptyList()
        }

        val result = mutableListOf<EndpointInfo>()

        val grpcServerInheritorsPointers: List<SmartPsiElementPointer<PsiClass>> = runInReadAccessInSmartModeWithResultAndRetryIgnorePCE(project) {
            getBindableServiceAnnotationClass()?.let { psiClass ->
                val inheritors = ClassInheritorsSearch.search(psiClass, searchScopeProvider.get(), true)
                inheritors.findAll().map { SmartPointerManager.getInstance(project).createSmartPsiElementPointer(it) }
            } ?: listOf()
        }


        grpcServerInheritorsPointers.forEach { currGrpcInheritorPointer ->

            executeCatchingWithRetryIgnorePCE({

                val psiClass = runInReadAccessWithResult { currGrpcInheritorPointer.element }

                runInReadAccessWithRetryIgnorePCE {
                    psiClass?.takeIf { psiClass.isValid && !isBaseClass(psiClass) }?.let { grpcInheritor ->
                        val endpointsAtClass = addEndpointMethods(grpcInheritor, context)
                        result.addAll(endpointsAtClass)
                    }
                }

            }, { e ->
                context.addError(getName(), e)
            })

        }

        return result

    }


    private fun addEndpointMethods(grpcServerClass: PsiClass, context: ProcessContext): List<EndpointInfo> {

        val grpcServiceName: String = evaluateServiceName(grpcServerClass)
        Log.log(
            logger::trace,
            "addEndpointMethods for grpcServerClass fqn='{}' with evaluated serviceName='{}'",
            grpcServerClass.qualifiedName,
            grpcServiceName
        )

        val result = mutableListOf<EndpointInfo>()
        val psiMethods = getMethodsInClass(grpcServerClass)

        psiMethods.forEach { method ->

            executeCatchingWithRetryIgnorePCE({

                val methodCodeObjectId = createPsiMethodCodeObjectId(method)
                val endpointId = createEndpointId(grpcServiceName, method)
                ////TODO: maybe search for parameters of type io.grpc.stub.StreamObserver , PsiParameterList parameterList = currPsiMethod.getParameterList();
                val endpointInfo = EndpointInfo(endpointId, methodCodeObjectId, toFileUri(method), null, EndpointFramework.Grpc)
                result.add(endpointInfo)

            }, { e ->
                context.addError(getName(), e)
            })
        }

        return result
    }


    private fun evaluateServiceName(grpcServerClass: PsiClass): String {

        val generatedGrpcBasePsiClass = climbUpToBaseClass(grpcServerClass) // for example GreeterGrpc.GreeterImplBase
        val generatedGrpcContainingPsiClass = generatedGrpcBasePsiClass.containingClass // for example GreeterGrpc
        if (generatedGrpcContainingPsiClass == null) {
            Log.log(
                logger::trace,
                "evaluateServiceName:#PotentialBug: could not find containing (generated) class for generated GRPC ImplBase Class fqn='{}'",
                generatedGrpcBasePsiClass.qualifiedName
            )
            return DIGMA_UNKNOWN_SERVICE_NAME // very unlikely
        }

        // searching for generated field named SERVICE_NAME, for example :
        // public static final String SERVICE_NAME = "helloworld.Greeter"
        val serviceNamePsiField = generatedGrpcContainingPsiClass.findFieldByName("SERVICE_NAME", false)
        if (serviceNamePsiField == null) {
            Log.log(
                logger::trace,
                "evaluateServiceName:#PotentialBug: could not find field 'SERVICE_NAME' in containing (generated) class of GRPC fqn='{}'",
                generatedGrpcContainingPsiClass.qualifiedName
            )
            return DIGMA_UNKNOWN_SERVICE_NAME // very unlikely
        }

        val serviceNameValueAsObj = serviceNamePsiField.computeConstantValue()
        if (serviceNameValueAsObj == null) {
            Log.log(
                logger::trace,
                "evaluateServiceName:#PotentialBug: could not evaluate value of field 'SERVICE_NAME' in containing (generated) class of GRPC fqn='{}', with field={}",
                generatedGrpcContainingPsiClass.qualifiedName,
                serviceNamePsiField
            )
            return DIGMA_UNKNOWN_SERVICE_NAME // very unlikely
        }

        return serviceNameValueAsObj.toString()
    }


    private fun createEndpointId(grpcServiceName: String, psiMethod: PsiMethod): String {
        //value for example: "epRPC:helloworld.Greeter/SayHello"
        return "" +  // digma part
                "epRPC:" +  // GRPC part
                grpcServiceName + "/" + capitalizeFirstLetter(psiMethod.name)
    }

    private fun capitalizeFirstLetter(value: String): String {
        val firstCharUcase = value[0].uppercaseChar()
        return firstCharUcase.toString() + value.substring(1)
    }
}