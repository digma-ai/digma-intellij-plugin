@file:Suppress("PrivatePropertyName")

package org.digma.intellij.plugin.idea.discovery.endpoint

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.impl.VirtualFileEnumeration
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.concurrency.annotations.RequiresReadLock
import kotlinx.coroutines.ensureActive
import org.digma.intellij.plugin.common.SearchScopeProvider
import org.digma.intellij.plugin.common.TextRangeUtils
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.common.isValidVirtualFile
import org.digma.intellij.plugin.idea.discovery.PsiPointers
import org.digma.intellij.plugin.idea.discovery.createPsiMethodCodeObjectId
import org.digma.intellij.plugin.idea.discovery.findAnnotatedMethods
import org.digma.intellij.plugin.idea.discovery.findNearestAnnotation
import org.digma.intellij.plugin.idea.discovery.getMethodsInClass
import org.digma.intellij.plugin.idea.discovery.toFileUri
import org.digma.intellij.plugin.idea.psi.java.JavaLanguageUtils
import org.digma.intellij.plugin.model.discovery.EndpointFramework
import org.digma.intellij.plugin.model.discovery.EndpointInfo
import org.digma.intellij.plugin.psi.PsiUtils
import java.util.Locale
import kotlin.coroutines.coroutineContext


abstract class AbstractJaxrsFrameworkEndpointDiscover(private val project: Project, jaxRsPackageName: String) : EndpointDiscovery() {

    protected abstract fun getFramework(): EndpointFramework

    private val applicationPathAnnotationClassFqn = jaxRsPackageName.plus(".ApplicationPath")
    private val jaxRsPathAnnotationStr = jaxRsPackageName.plus(".Path")

    private val HTTP_DELETE_ANNOTATION_STR = jaxRsPackageName.plus(".DELETE")
    private val HTTP_GET_ANNOTATION_STR = jaxRsPackageName.plus(".GET")
    private val HTTP_HEAD_ANNOTATION_STR = jaxRsPackageName.plus(".HEAD")
    private val HTTP_OPTIONS_ANNOTATION_STR = jaxRsPackageName.plus(".OPTIONS")
    private val HTTP_PATCH_ANNOTATION_STR = jaxRsPackageName.plus(".PATCH")
    private val HTTP_POST_ANNOTATION_STR = jaxRsPackageName.plus(".POST")
    private val HTTP_PUT_ANNOTATION_STR = jaxRsPackageName.plus(".PUT")

    private val httpMethodsAnnotationStrList = listOf(
        HTTP_DELETE_ANNOTATION_STR, HTTP_GET_ANNOTATION_STR, HTTP_HEAD_ANNOTATION_STR, HTTP_OPTIONS_ANNOTATION_STR,
        HTTP_PATCH_ANNOTATION_STR, HTTP_POST_ANNOTATION_STR, HTTP_PUT_ANNOTATION_STR
    )


    private fun getPathAnnotationClass(): PsiClass? {
        return project.service<PsiPointers>().getPsiClass(project, jaxRsPathAnnotationStr)
    }


    private fun isJaxRsHttpRelevant(): Boolean {
        return getPathAnnotationClass() != null
    }


    /**
     * Overriding since have different logic,
     * in this impl searching for classes/methods in file and see if they inherit from super classes which declare on @PATH annotations.
     */
    private suspend fun lookForEndpoints(psiFile: PsiFile): List<EndpointInfo> {

        if (!isJaxRsHttpRelevant() || !PsiUtils.isValidPsiFile(psiFile) || !isProjectValid(project)) {
            return emptyList()
        }

        val endpointInfos = mutableListOf<EndpointInfo>()

        coroutineContext.ensureActive()
        val allClassesInFile: List<SmartPsiElementPointer<PsiClass>> =
            smartReadAction(project) {
                val classes = PsiTreeUtil.findChildrenOfType(psiFile, PsiClass::class.java)
                classes.map { SmartPointerManager.getInstance(project).createSmartPsiElementPointer(it) }
            }


        allClassesInFile.forEach { psiClassPointer ->
            coroutineContext.ensureActive()
            val psiClass = readAction { psiClassPointer.element }

            psiClass?.let { cls ->
                val methodsInClass = readAction { getMethodsInClass(cls) }

                methodsInClass.forEach { psiMethod ->
                    coroutineContext.ensureActive()
                    val candidateMethods = mutableSetOf<PsiMethod>()
                    val methodPathAnnotation = readAction { findNearestAnnotation(psiMethod, jaxRsPathAnnotationStr) }
                    coroutineContext.ensureActive()
                    val hasPath = methodPathAnnotation != null
                    val hasHttpMethod = httpMethodsAnnotationStrList.any { annotationFqn ->
                        coroutineContext.ensureActive()
                        readAction {
                            findNearestAnnotation(psiMethod, annotationFqn) != null
                        }
                    }
                    coroutineContext.ensureActive()
                    if (hasPath || hasHttpMethod) {
                        candidateMethods.add(psiMethod)
                        endpointInfos.addAll(handleCandidateMethods(candidateMethods).stream().toList())
                    }
                }
            }
        }
        return endpointInfos
    }


    override suspend fun lookForEndpoints(searchScopeProvider: SearchScopeProvider): List<EndpointInfo> {

        if (!isJaxRsHttpRelevant() || !isProjectValid(project)) {
            return emptyList()
        }

        //todo: IMO there should be no special handling for file , just use the search scope.
        //jax-rs need special discovery for PsiFile
        val psiFile = extractPsiFileIfFileScope(searchScopeProvider)
        if (psiFile != null && PsiUtils.isValidPsiFile(psiFile)) {
            return lookForEndpoints(psiFile)
        }

        val psiPointers = project.service<PsiPointers>()

        val endpointInfos: MutableList<EndpointInfo> = ArrayList()

        httpMethodsAnnotationStrList.forEach { annotationFqn ->
            coroutineContext.ensureActive()
            psiPointers.getPsiClass(project, annotationFqn)?.let {
                val annotatedMethods: List<SmartPsiElementPointer<PsiMethod>>? =
                    psiPointers.getPsiClassPointer(project, annotationFqn)?.let { annotationClassPointer ->
                        findAnnotatedMethods(project, annotationClassPointer, searchScopeProvider)
                    }
                coroutineContext.ensureActive()
                annotatedMethods?.forEach { annotatedMethodPointer ->
                    coroutineContext.ensureActive()
                    val annotatedMethod = readAction { annotatedMethodPointer.element }
                    annotatedMethod?.let {
                        coroutineContext.ensureActive()
                        val candidateMethods = mutableSetOf<PsiMethod>()
                        candidateMethods.add(annotatedMethod)
                        val overridingMethods = smartReadAction(project) {
                            OverridingMethodsSearch.search(annotatedMethod).findAll()
                        }
                        coroutineContext.ensureActive()
                        candidateMethods.addAll(overridingMethods)
                        endpointInfos.addAll(handleCandidateMethods(candidateMethods).toList())
                    }
                }
            }
        }

        return endpointInfos
    }


    @Suppress("UnstableApiUsage")
    private suspend fun extractPsiFileIfFileScope(searchScopeProvider: SearchScopeProvider): PsiFile? {
        return readAction {
            val scope = searchScopeProvider.get()
            if (scope is GlobalSearchScope) {
                val virtualFileEnumeration = VirtualFileEnumeration.extract(scope)
                if (virtualFileEnumeration != null) {
                    val filesCollection = virtualFileEnumeration.filesIfCollection
                    if (filesCollection != null) {
                        val virtualFile = filesCollection.stream().findFirst().orElse(null)
                        if (isValidVirtualFile(virtualFile)) {
                            return@readAction PsiManager.getInstance(project).findFile(virtualFile)
                        }
                        return@readAction null
                    }
                }
            }
            null
        }
    }


    private suspend fun handleCandidateMethods(candidateMethods: Collection<PsiMethod>): Set<EndpointInfo> {
        val retSet = mutableSetOf<EndpointInfo>()
        candidateMethods.forEach { currPsiMethod ->
            coroutineContext.ensureActive()
            val methodPathAnnotation = readAction { findNearestAnnotation(currPsiMethod, jaxRsPathAnnotationStr) }
            coroutineContext.ensureActive()
            val currClass = readAction { currPsiMethod.containingClass }
            val controllerPathAnnotation = if (currClass == null) null else readAction { findNearestAnnotation(currClass, jaxRsPathAnnotationStr) }
            coroutineContext.ensureActive()
            // skip if we couldn't find annotation of @Path, in either class and method
            if (methodPathAnnotation != null || controllerPathAnnotation != null) {
                val appPaths = readAction { evaluateApplicationPaths(currPsiMethod) }
                coroutineContext.ensureActive()
                httpMethodsAnnotationStrList.forEach { annotationFqn ->
                    coroutineContext.ensureActive()
                    val httpMethodAnnotation = readAction { findNearestAnnotation(currPsiMethod, annotationFqn) }
                    httpMethodAnnotation?.let {
                        val endpointSuffixUri = readAction { combinePaths(controllerPathAnnotation, methodPathAnnotation) }
                        appPaths.forEach { appPath ->
                            coroutineContext.ensureActive()
                            readAction {
                                val endpointFullUri = combineUri(appPath, endpointSuffixUri)
                                val httpEndpointCodeObjectId = createHttpEndpointCodeObjectId(annotationFqn, endpointFullUri)
                                val textRange =
                                    TextRangeUtils.fromJBTextRange(currPsiMethod.textRange)
                                val endpointInfo = EndpointInfo(
                                    httpEndpointCodeObjectId,
                                    createPsiMethodCodeObjectId(currPsiMethod),
                                    toFileUri(currPsiMethod),
                                    textRange,
                                    getFramework()
                                )
                                retSet.add(endpointInfo)
                            }
                        }
                    }
                }
            }
        }
        return retSet
    }


    @RequiresReadLock(generateAssertion = false)
    private fun evaluateApplicationPaths(psiElement: PsiElement): Set<String?> {

        var appPathAnnotationClass = project.service<PsiPointers>().getPsiClass(project, applicationPathAnnotationClassFqn)
        val appPaths: MutableSet<String?> = HashSet()
        appPathAnnotationClass?.let {
            // check for ApplicationPath in context of module
            val module = ModuleUtilCore.findModuleForPsiElement(psiElement)
            if (module != null) {
                //get the class again, maybe it's invalidated
                appPathAnnotationClass = project.service<PsiPointers>().getPsiClass(project, applicationPathAnnotationClassFqn)
                appPathAnnotationClass?.let { annotationClass ->
                    val appPathPsiClasses = AnnotatedElementsSearch.searchPsiClasses(annotationClass, GlobalSearchScope.moduleScope(module))
                    appPathPsiClasses.forEach { appPathClass ->
                        val appPathAnnotation = appPathClass.getAnnotation(applicationPathAnnotationClassFqn)
                        appPathAnnotation?.let {
                            val appPathValue = JavaLanguageUtils.getPsiAnnotationAttributeValue(it, "value")
                            appPaths.add(appPathValue)
                        }
                    }
                }
            }
        }

        if (appPaths.isEmpty()) {
            appPaths.add("/") // make sure have at least one entry
        }
        return appPaths
    }


    private fun combinePaths(annotOfPrefix: PsiAnnotation?, annotOfSuffix: PsiAnnotation?): String {
        var prefixStr: String? = ""
        if (annotOfPrefix != null) {
            prefixStr = JavaLanguageUtils.getPsiAnnotationAttributeValue(annotOfPrefix, "value")
        }
        var suffixStr: String? = ""
        if (annotOfSuffix != null) {
            suffixStr = JavaLanguageUtils.getPsiAnnotationAttributeValue(annotOfSuffix, "value")
        }
        return combineUri(prefixStr, suffixStr)
    }

    private fun createHttpEndpointCodeObjectId(annotationFqn: String, endpointFullUri: String): String {
        val httpMethodUcase = getHttpMethod(annotationFqn).uppercase(Locale.getDefault())

        // value, for example, 'epHTTP:HTTP GET /books/get'
        return "" +  // digma part
                "epHTTP:" + "HTTP " + httpMethodUcase + " " +  // JaxRs part
                endpointFullUri
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
