package org.digma.intellij.plugin.idea.discovery.endpoint

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.util.concurrency.annotations.RequiresReadLock
import kotlinx.coroutines.ensureActive
import org.digma.intellij.plugin.common.SearchScopeProvider
import org.digma.intellij.plugin.common.TextRangeUtils
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.idea.discovery.PsiPointers
import org.digma.intellij.plugin.idea.discovery.climbUpToBaseClass
import org.digma.intellij.plugin.idea.discovery.createPsiMethodCodeObjectId
import org.digma.intellij.plugin.idea.discovery.getMethodsInClass
import org.digma.intellij.plugin.idea.discovery.isBaseClass
import org.digma.intellij.plugin.idea.discovery.toFileUri
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.EndpointFramework
import org.digma.intellij.plugin.model.discovery.EndpointInfo
import kotlin.coroutines.coroutineContext

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


    override suspend fun lookForEndpoints(searchScopeProvider: SearchScopeProvider): List<EndpointInfo>? {

        if (!isGrpcServerRelevant() || !isProjectValid(project)) {
            return emptyList()
        }

        val result = mutableListOf<EndpointInfo>()
        coroutineContext.ensureActive()
        val grpcServerInheritorsPointers: List<SmartPsiElementPointer<PsiClass>> = smartReadAction(project) {
            getBindableServiceAnnotationClass()?.let { psiClass ->
                val inheritors = ClassInheritorsSearch.search(psiClass, searchScopeProvider.get(), true)
                inheritors.findAll().map { SmartPointerManager.getInstance(project).createSmartPsiElementPointer(it) }
            } ?: listOf()
        }


        grpcServerInheritorsPointers.forEach { currGrpcInheritorPointer ->
            coroutineContext.ensureActive()
            readAction {
                val psiClass = currGrpcInheritorPointer.element
                psiClass?.takeIf { psiClass.isValid && !isBaseClass(psiClass) }?.let { grpcInheritor ->
                    val endpointsAtClass = addEndpointMethods(grpcInheritor)
                    result.addAll(endpointsAtClass)
                }
            }
        }

        return result

    }


    @RequiresReadLock(generateAssertion = false)
    private fun addEndpointMethods(grpcServerClass: PsiClass): List<EndpointInfo> {

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
            val methodCodeObjectId = createPsiMethodCodeObjectId(method)
            val endpointId = createEndpointId(grpcServiceName, method)
            ////TODO: maybe search for parameters of type io.grpc.stub.StreamObserver , PsiParameterList parameterList = currPsiMethod.getParameterList();
            val textRange =
                TextRangeUtils.fromJBTextRange(method.textRange)
            val endpointInfo = EndpointInfo(endpointId, methodCodeObjectId, toFileUri(method), textRange, EndpointFramework.Grpc)
            result.add(endpointInfo)

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
                grpcServiceName + "/" + psiMethod.name.replaceFirstChar { it.uppercase() }
    }
}