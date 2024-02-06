package org.digma.intellij.plugin.idea.psi.discovery.endpoint

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderEntry
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.util.text.VersionComparatorUtil
import org.digma.intellij.plugin.common.SearchScopeProvider
import org.digma.intellij.plugin.common.executeCatchingWithRetryIgnorePCE
import org.digma.intellij.plugin.common.runInReadAccess
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.deps.ModulesDepsService.Companion.toUnifiedCoordinates
import org.digma.intellij.plugin.idea.psi.PsiPointers
import org.digma.intellij.plugin.idea.psi.createPsiMethodCodeObjectId
import org.digma.intellij.plugin.idea.psi.findAnnotatedMethods
import org.digma.intellij.plugin.idea.psi.java.JavaLanguageUtils
import org.digma.intellij.plugin.idea.psi.java.JavaPsiUtils.Companion.toFileUri
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.EndpointFramework
import org.digma.intellij.plugin.model.discovery.EndpointInfo
import org.digma.intellij.plugin.progress.ProcessContext
import java.util.Arrays
import java.util.Locale

private const val CONTROLLER_ANNOTATION_STR = "io.micronaut.http.annotation.Controller"
private const val HTTP_DELETE_ANNOTATION_STR = "io.micronaut.http.annotation.Delete"
private const val HTTP_GET_ANNOTATION_STR = "io.micronaut.http.annotation.Get"
private const val HTTP_HEAD_ANNOTATION_STR = "io.micronaut.http.annotation.Head"
private const val HTTP_OPTIONS_ANNOTATION_STR = "io.micronaut.http.annotation.Options"
private const val HTTP_PATCH_ANNOTATION_STR = "io.micronaut.http.annotation.Patch"
private const val HTTP_POST_ANNOTATION_STR = "io.micronaut.http.annotation.Post"
private const val HTTP_PUT_ANNOTATION_STR = "io.micronaut.http.annotation.Put"
private const val HTTP_TRACE_ANNOTATION_STR = "io.micronaut.http.annotation.Trace"


class MicronautFrameworkEndpointDiscovery(private val project: Project) : EndpointDiscovery() {

    private val logger = Logger.getInstance(this::class.java)


    companion object {
        @JvmStatic
        val HTTP_METHODS_ANNOTATION_STR_LIST = listOf(
            HTTP_DELETE_ANNOTATION_STR,
            HTTP_GET_ANNOTATION_STR,
            HTTP_HEAD_ANNOTATION_STR,
            HTTP_OPTIONS_ANNOTATION_STR,
            HTTP_PATCH_ANNOTATION_STR,
            HTTP_POST_ANNOTATION_STR,
            HTTP_PUT_ANNOTATION_STR,
            HTTP_TRACE_ANNOTATION_STR
        )

        private val VALUE_OR_URI = listOf("value", "uri")

    }

    override fun getName(): String {
        return "Micronaut"
    }


    private fun getControllerAnnotationClass(): PsiClass? {
        return project.service<PsiPointers>().getPsiClass(project, CONTROLLER_ANNOTATION_STR)
    }


    private fun isMicronautHttpRelevant(): Boolean {
        return getControllerAnnotationClass() != null
    }


    //todo: until all frameworks implement the other method and ProcessContext is sent from all processes
    override fun lookForEndpoints(searchScopeProvider: SearchScopeProvider): List<EndpointInfo> {
        return lookForEndpoints(searchScopeProvider, null)
    }

    override fun lookForEndpoints(searchScopeProvider: SearchScopeProvider, context: ProcessContext?): List<EndpointInfo> {

        if (!isMicronautHttpRelevant()) {
            return listOf()
        }

        val psiPointers = project.service<PsiPointers>()

        val resultEndpoints = mutableListOf<EndpointInfo>()

        //any iterations catch exceptions and continue to the next iterations so not the whole
        // process is crashed

        HTTP_METHODS_ANNOTATION_STR_LIST.forEach { annotationFqn ->

            context?.indicator?.checkCanceled()

            //catch any exception for each annotation
            executeCatchingWithRetryIgnorePCE({

                val annotationClass = psiPointers.getPsiClass(project, annotationFqn)

                annotationClass?.let {

                    val annotatedMethods = findAnnotatedMethods(project, annotationClass, searchScopeProvider)

                    annotatedMethods.forEach { annotatedMethod: SmartPsiElementPointer<PsiMethod> ->

                        context?.indicator?.checkCanceled()

                        //catch any exception for each annotation
                        executeCatchingWithRetryIgnorePCE({
                            //start read access for each annotated method
                            runInReadAccess {
                                buildEndpointsForAnnotatedMethod(resultEndpoints, annotationFqn, annotatedMethod)
                            }
                        }, { e ->
                            ErrorReporter.getInstance().reportError("SpringBootFrameworkEndpointDiscovery.lookForEndpoints", e)
                            context?.addError(getName(), e)
                        })
                    }
                }
            }, { e ->
                ErrorReporter.getInstance().reportError("SpringBootFrameworkEndpointDiscovery.lookForEndpoints", e)
                context?.addError(getName(), e)
            })

        }
        return resultEndpoints
    }

    private fun buildEndpointsForAnnotatedMethod(
        resultEndpoints: MutableList<EndpointInfo>,
        annotationFqn: String,
        annotatedMethod: SmartPsiElementPointer<PsiMethod>,
    ) {

        annotatedMethod.element?.let { psiMethod ->

            val controllerClass: PsiClass = psiMethod.containingClass
                ?: return  // very unlikely

            val controllerAnnotation = controllerClass.getAnnotation(CONTROLLER_ANNOTATION_STR)
                ?: return  // skip this method, since its class is not a controller

            val endpointUriPrefix = JavaLanguageUtils.getPsiAnnotationAttributeValue(controllerAnnotation, "value") ?: ""

            val methodCodeObjectId = createPsiMethodCodeObjectId(psiMethod)
            val httpEndpointCodeObjectId: String = createHttpEndpointCodeObjectId(psiMethod, annotationFqn, endpointUriPrefix)
                ?: return  // skip this method, since endpoint value could not be determined

            val endpointInfo = EndpointInfo(httpEndpointCodeObjectId, methodCodeObjectId, toFileUri(psiMethod), null, EndpointFramework.Micronaut)
            Log.log(logger::debug, "Found endpoint info '{}' for method '{}'", endpointInfo.id, endpointInfo.containingMethodId)
            resultEndpoints.add(endpointInfo)
        }
    }


    private fun createHttpEndpointCodeObjectId(psiMethod: PsiMethod, annotationFqn: String, endpointUriPrefix: String): String? {
        val httpPsiAnnotation = psiMethod.getAnnotation(annotationFqn) ?: return null

        val annotationAttributes = httpPsiAnnotation.attributes

        var endpointUriSuffix: String? = "/"

        for (currAttribute in annotationAttributes) {
            // taking the first attribute, either "value" or "uri" - that's how micronaut behave if both exists
            if (VALUE_OR_URI.contains(currAttribute.attributeName)) {
                endpointUriSuffix = JavaLanguageUtils.getPsiAnnotationAttributeValue(httpPsiAnnotation, currAttribute.attributeName)
                if (endpointUriSuffix == null) {
                    Log.log(
                        logger::debug,
                        "cannot create http endpoint for method '{}' since could not extract attribute value for name '{}' from annotation '{}'",
                        psiMethod.name, currAttribute.attributeName, annotationFqn
                    )
                    return null // unlikely
                }
                break // found the first occurrence, and out
            }
            // note: attribute of "uris" is irrelevant
        }

        val httpMethodUcase = getHttpMethod(annotationFqn).uppercase(Locale.getDefault())

        // value for example : 'epHTTP:HTTP GET - /books/get'
        var endpointId =  // digma part
            "epHTTP:HTTP $httpMethodUcase "
        // Micronaut part
        endpointId = if (isMicronaut376OrBellow(psiMethod)) {
            //fix for micronaut 3.7.6 and bellow where the endpoint id should contain double GET GET
            endpointId + httpMethodUcase + " - " + combineUri(endpointUriPrefix, endpointUriSuffix)
        } else {
            endpointId + combineUri(endpointUriPrefix, endpointUriSuffix)
        }

        return endpointId
    }


    private fun isMicronaut376OrBellow(psiMethod: PsiMethod): Boolean {
        val module = ModuleUtilCore.findModuleForPsiElement(psiMethod) ?: return false

        val moduleRootManager = ModuleRootManager.getInstance(module)
        val orderEntries = moduleRootManager.orderEntries

        val micronautLib = Arrays.stream(orderEntries)
            .filter { orderEntry: OrderEntry? ->
                (orderEntry is LibraryOrderEntry) && orderEntry.libraryName != null &&
                        orderEntry.libraryName!!.contains("io.micronaut:micronaut-core")
            }
            .findFirst().orElse(null)

        if (micronautLib == null) {
            return false
        }

        val micronautVersion = toUnifiedCoordinates((micronautLib as LibraryOrderEntry)).version
        return VersionComparatorUtil.compare("3.7.6", micronautVersion) >= 0


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


    private fun getHttpMethod(annotationFqn: String): String {
        val lastIndexOfDot = annotationFqn.lastIndexOf('.')
        return if (lastIndexOfDot >= 0) {
            annotationFqn.substring(lastIndexOfDot + 1)
        } else {
            annotationFqn
        }
    }

}