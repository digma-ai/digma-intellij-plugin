package org.digma.intellij.plugin.idea.discovery.endpoint

import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.SmartPsiElementPointer
import kotlinx.coroutines.ensureActive
import org.digma.intellij.plugin.common.SearchScopeProvider
import org.digma.intellij.plugin.common.TextRangeUtils
import org.digma.intellij.plugin.idea.discovery.PsiPointers
import org.digma.intellij.plugin.idea.discovery.createPsiMethodCodeObjectId
import org.digma.intellij.plugin.idea.discovery.findAnnotatedMethods
import org.digma.intellij.plugin.idea.discovery.hasOneOfAnnotations
import org.digma.intellij.plugin.idea.discovery.toFileUri
import org.digma.intellij.plugin.idea.psi.java.JavaLanguageUtils
import org.digma.intellij.plugin.model.discovery.EndpointFramework
import org.digma.intellij.plugin.model.discovery.EndpointInfo
import java.util.Locale
import kotlin.coroutines.coroutineContext

private const val CONTROLLER_ANNOTATION_STR = "org.springframework.stereotype.Controller"
private const val REST_CONTROLLER_ANNOTATION_STR = "org.springframework.web.bind.annotation.RestController"

private const val HTTP_DELETE_ANNOTATION_STR = "org.springframework.web.bind.annotation.DeleteMapping"
private const val HTTP_GET_ANNOTATION_STR = "org.springframework.web.bind.annotation.GetMapping"
private const val HTTP_PATCH_ANNOTATION_STR = "org.springframework.web.bind.annotation.PatchMapping"
private const val HTTP_POST_ANNOTATION_STR = "org.springframework.web.bind.annotation.PostMapping"
private const val HTTP_PUT_ANNOTATION_STR = "org.springframework.web.bind.annotation.PutMapping"
private const val HTTP_REQUEST_MAPPING_ANNOTATION_STR = "org.springframework.web.bind.annotation.RequestMapping"


class SpringBootFrameworkEndpointDiscovery(private val project: Project) : EndpointDiscovery() {

    companion object {
        @JvmStatic
        val HTTP_METHODS_ANNOTATION_STR_LIST = listOf(
            HTTP_DELETE_ANNOTATION_STR,
            HTTP_GET_ANNOTATION_STR,
            HTTP_PATCH_ANNOTATION_STR,
            HTTP_POST_ANNOTATION_STR,
            HTTP_PUT_ANNOTATION_STR,
            HTTP_REQUEST_MAPPING_ANNOTATION_STR
        )


        val ANNOTATION_TO_HTTP_METHOD = mapOf(
            HTTP_DELETE_ANNOTATION_STR to "DELETE",
            HTTP_GET_ANNOTATION_STR to "GET",
            HTTP_PATCH_ANNOTATION_STR to "PATCH",
            HTTP_POST_ANNOTATION_STR to "POST",
            HTTP_PUT_ANNOTATION_STR to "PUT"
        )

        val ATTRIBUTES_OF_PATH = listOf("value", "path")
    }

    override fun getName(): String {
        return "SpringBoot"
    }


    private fun getControllerAnnotationClass(): PsiClass? {
        return project.service<PsiPointers>().getPsiClass(project, CONTROLLER_ANNOTATION_STR)
    }


    private fun isSpringBootWebRelevant(): Boolean {
        return getControllerAnnotationClass() != null
    }


    override suspend fun lookForEndpoints(searchScopeProvider: SearchScopeProvider): List<EndpointInfo> {

        if (!isSpringBootWebRelevant()) {
            return listOf()
        }

        val psiPointers = project.service<PsiPointers>()

        val resultEndpoints = mutableListOf<EndpointInfo>()

        //the iterations catch exceptions and continue to the next iterations so not the whole
        // process is crashed
        coroutineContext.ensureActive()
        HTTP_METHODS_ANNOTATION_STR_LIST.forEach { annotationFqn ->
            coroutineContext.ensureActive()

            psiPointers.getPsiClass(project, annotationFqn)?.let {
                val annotatedMethods = psiPointers.getPsiClassPointer(project, annotationFqn)?.let { annotationClassPointer ->
                    findAnnotatedMethods(project, annotationClassPointer, searchScopeProvider)
                }

                annotatedMethods?.forEach { annotatedMethod: SmartPsiElementPointer<PsiMethod> ->
                    coroutineContext.ensureActive()
                    //start read access for each annotated method
                    readAction {
                        buildEndpointsForAnnotatedMethod(resultEndpoints, annotationFqn, annotatedMethod)
                    }
                }
            }

        }
        return resultEndpoints
    }

    private fun buildEndpointsForAnnotatedMethod(
        resultEndpoints: MutableList<EndpointInfo>,
        annotationFqn: String,
        annotatedMethod: SmartPsiElementPointer<PsiMethod>,
    ) {

        annotatedMethod.element?.let { psiMethod ->

            val methodId = createPsiMethodCodeObjectId(psiMethod)
            val mappingPsiAnnotationOnMethod: PsiAnnotation = psiMethod.getAnnotation(annotationFqn)
                ?: return  // very unlikely

            val controllerClass: PsiClass = psiMethod.containingClass
                ?: return  // very unlikely

            if (!hasOneOfAnnotations(
                    controllerClass,
                    CONTROLLER_ANNOTATION_STR,
                    REST_CONTROLLER_ANNOTATION_STR
                )
            ) {
                return  // skip this method, since its class is not a controller (or rest controller)
            }

            val controllerReqMappingAnnotation = controllerClass.getAnnotation(HTTP_REQUEST_MAPPING_ANNOTATION_STR)

            val endpointUriPrefix = if (controllerReqMappingAnnotation != null) {
                JavaLanguageUtils.getValueOfFirstMatchingAnnotationAttribute(controllerReqMappingAnnotation, ATTRIBUTES_OF_PATH, "")
            } else {
                ""
            }

            val httpMethodName: String = evalHttpMethod(mappingPsiAnnotationOnMethod, controllerReqMappingAnnotation)
                ?: return  // not likely

            val endpointUriSuffixes =
                JavaLanguageUtils.getValuesOfFirstMatchingAnnotationAttribute(mappingPsiAnnotationOnMethod, ATTRIBUTES_OF_PATH)

            for (currSuffix in endpointUriSuffixes) {
                val httpEndpointCodeObjectId = createHttpEndpointCodeObjectId(httpMethodName, endpointUriPrefix, currSuffix)
                val textRange =
                    TextRangeUtils.fromJBTextRange(psiMethod.textRange)
                val endpointInfo = EndpointInfo(httpEndpointCodeObjectId, methodId, toFileUri(psiMethod), textRange, EndpointFramework.SpringBoot)
                resultEndpoints.add(endpointInfo)
            }
        }

    }


    private fun evalHttpMethod(mappingPsiAnnotationOnMethod: PsiAnnotation, controllerReqMappingAnnotation: PsiAnnotation?): String? {
        val mappedValue = ANNOTATION_TO_HTTP_METHOD[mappingPsiAnnotationOnMethod.qualifiedName]
        if (mappedValue != null) {
            return mappedValue
        }

        if (HTTP_REQUEST_MAPPING_ANNOTATION_STR == mappingPsiAnnotationOnMethod.qualifiedName) {
            // trying for attribute "method" in on method annotation
            run {
                val value = JavaLanguageUtils.getPsiAnnotationAttributeValue(mappingPsiAnnotationOnMethod, "method")
                if (value != null) {
                    return value.uppercase(Locale.getDefault())
                }
            }
            if (controllerReqMappingAnnotation != null) {
                // fallback to attribute "method" in on controller annotation
                val value = JavaLanguageUtils.getPsiAnnotationAttributeValue(controllerReqMappingAnnotation, "method")
                if (value != null) {
                    return value.uppercase(Locale.getDefault())
                }
            }
        }

        return null
    }


    private fun createHttpEndpointCodeObjectId(httpMethod: String, endpointUriPrefix: String?, endpointUriSuffix: String?): String {
        // value for example : 'epHTTP:HTTP GET /books/get'
        return "" +  // digma part
                "epHTTP:" + "HTTP " + httpMethod.uppercase(Locale.getDefault()) + " " +  // Spring Web part
                combineUri(endpointUriPrefix, endpointUriSuffix)
    }

}