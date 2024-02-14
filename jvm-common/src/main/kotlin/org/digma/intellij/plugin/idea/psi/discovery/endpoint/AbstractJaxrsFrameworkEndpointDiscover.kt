@file:Suppress("PrivatePropertyName")

package org.digma.intellij.plugin.idea.psi.discovery.endpoint

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
import org.digma.intellij.plugin.common.SearchScopeProvider
import org.digma.intellij.plugin.common.executeCatchingIgnorePCE
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.common.isValidVirtualFile
import org.digma.intellij.plugin.common.runInReadAccessInSmartModeWithResultAndRetryIgnorePCE
import org.digma.intellij.plugin.common.runInReadAccessWithResult
import org.digma.intellij.plugin.common.runInReadAccessWithRetryIgnorePCE
import org.digma.intellij.plugin.idea.psi.PsiPointers
import org.digma.intellij.plugin.idea.psi.createPsiMethodCodeObjectId
import org.digma.intellij.plugin.idea.psi.findAnnotatedMethods
import org.digma.intellij.plugin.idea.psi.findNearestAnnotation
import org.digma.intellij.plugin.idea.psi.getMethodsInClass
import org.digma.intellij.plugin.idea.psi.java.JavaLanguageUtils
import org.digma.intellij.plugin.idea.psi.toFileUri
import org.digma.intellij.plugin.model.discovery.EndpointFramework
import org.digma.intellij.plugin.model.discovery.EndpointInfo
import org.digma.intellij.plugin.progress.ProcessContext
import org.digma.intellij.plugin.psi.PsiUtils
import java.util.Locale


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
    private fun lookForEndpoints(psiFile: PsiFile, context: ProcessContext): List<EndpointInfo> {

        if (!isJaxRsHttpRelevant() || !PsiUtils.isValidPsiFile(psiFile) || !isProjectValid(project)) {
            return emptyList()
        }

        val endpointInfos = mutableListOf<EndpointInfo>()

        val allClassesInFile: List<SmartPsiElementPointer<PsiClass>> =
            runInReadAccessInSmartModeWithResultAndRetryIgnorePCE(project) {
                val classes = PsiTreeUtil.findChildrenOfType(psiFile, PsiClass::class.java)
                classes.map { SmartPointerManager.getInstance(project).createSmartPsiElementPointer(it) }
            }


        allClassesInFile.forEach { psiClassPointer ->

            val psiClass = runInReadAccessWithResult { psiClassPointer.element }

            psiClass?.let { cls ->
                val methodsInClass = runInReadAccessWithResult { getMethodsInClass(cls) }

                methodsInClass.forEach { psiMethod ->
                    runInReadAccessWithRetryIgnorePCE {
                        val candidateMethods = mutableSetOf<PsiMethod>()
                        val methodPathAnnotation = runInReadAccessWithResult { findNearestAnnotation(psiMethod, jaxRsPathAnnotationStr) }
                        val hasPath = methodPathAnnotation != null
                        val hasHttpMethod = httpMethodsAnnotationStrList.any { annotationFqn ->
                            runInReadAccessWithResult {
                                findNearestAnnotation(psiMethod, annotationFqn) != null
                            }
                        }
                        if (hasPath || hasHttpMethod) {
                            candidateMethods.add(psiMethod)
                            endpointInfos.addAll(handleCandidateMethods(candidateMethods, context).stream().toList())
                        }
                    }
                }
            }
        }
        return endpointInfos
    }


    override fun lookForEndpoints(searchScopeProvider: SearchScopeProvider, context: ProcessContext): List<EndpointInfo> {

        if (!isJaxRsHttpRelevant() || !isProjectValid(project)) {
            return emptyList()
        }

        //todo: IMO there should be no special handling for file , just use the search scope.
        //jax-rs need special discovery for PsiFile
        val psiFile = extractPsiFileIfFileScope(searchScopeProvider)
        if (psiFile != null && PsiUtils.isValidPsiFile(psiFile)) {
            return lookForEndpoints(psiFile, context)
        }

        val psiPointers = project.service<PsiPointers>()

        val endpointInfos: MutableList<EndpointInfo> = ArrayList()

        httpMethodsAnnotationStrList.forEach { annotationFqn ->

            executeCatchingIgnorePCE({
                val annotationClass = psiPointers.getPsiClass(project, annotationFqn)
                annotationClass?.let {

                    val annotatedMethods: List<SmartPsiElementPointer<PsiMethod>> =
                        findAnnotatedMethods(project, annotationClass, searchScopeProvider)

                    annotatedMethods.forEach { annotatedMethodPointer ->
                        executeCatchingIgnorePCE({
                            runInReadAccessWithRetryIgnorePCE {
                                val annotatedMethod = annotatedMethodPointer.element
                                annotatedMethod?.let {
                                    val candidateMethods = mutableSetOf<PsiMethod>()
                                    candidateMethods.add(annotatedMethod)
                                    val overridingMethods = OverridingMethodsSearch.search(annotatedMethod)
                                    candidateMethods.addAll(overridingMethods.findAll())
                                    endpointInfos.addAll(handleCandidateMethods(candidateMethods, context).stream().toList())
                                }
                            }
                        }, { e ->
                            context.addError(getName(), e)
                        })
                    }
                }
            }, { e ->
                context.addError(getName(), e)
            })
        }

        return endpointInfos
    }

    @Suppress("UnstableApiUsage")
    private fun extractPsiFileIfFileScope(searchScopeProvider: SearchScopeProvider): PsiFile? {
        return runInReadAccessWithResult {
            val scope = searchScopeProvider.get()
            if (scope is GlobalSearchScope) {
                val virtualFileEnumeration = VirtualFileEnumeration.extract(scope)
                if (virtualFileEnumeration != null) {
                    val filesCollection = virtualFileEnumeration.filesIfCollection
                    if (filesCollection != null) {
                        val virtualFile = filesCollection.stream().findFirst().orElse(null)
                        if (isValidVirtualFile(virtualFile)) {
                            return@runInReadAccessWithResult PsiManager.getInstance(project).findFile(virtualFile)
                        }
                        return@runInReadAccessWithResult null
                    }
                }
            }
            null
        }
    }

    //this method run in read access already, may be long read access..
    private fun handleCandidateMethods(candidateMethods: Collection<PsiMethod>, context: ProcessContext): Set<EndpointInfo> {
        val retSet = mutableSetOf<EndpointInfo>()

        candidateMethods.forEach { currPsiMethod ->

            executeCatchingIgnorePCE({

                val methodPathAnnotation = findNearestAnnotation(currPsiMethod, jaxRsPathAnnotationStr)

                val currClass = currPsiMethod.containingClass
                val controllerPathAnnotation = if (currClass == null) null else findNearestAnnotation(currClass, jaxRsPathAnnotationStr)

                // skip if couldn't find annotation of @Path, in either class and method
                if (methodPathAnnotation != null || controllerPathAnnotation != null) {

                    val appPaths = evaluateApplicationPaths(currPsiMethod)

                    httpMethodsAnnotationStrList.forEach { annotationFqn ->

                        executeCatchingIgnorePCE({
                            val httpMethodAnnotation = findNearestAnnotation(currPsiMethod, annotationFqn)

                            httpMethodAnnotation?.let {
                                val endpointSuffixUri = combinePaths(controllerPathAnnotation, methodPathAnnotation)
                                appPaths.forEach { appPath ->
                                    val endpointFullUri = combineUri(appPath, endpointSuffixUri)
                                    val httpEndpointCodeObjectId = createHttpEndpointCodeObjectId(annotationFqn, endpointFullUri)

                                    val endpointInfo = EndpointInfo(
                                        httpEndpointCodeObjectId,
                                        createPsiMethodCodeObjectId(currPsiMethod),
                                        toFileUri(currPsiMethod),
                                        null,
                                        getFramework()
                                    )
                                    retSet.add(endpointInfo)
                                }
                            }
                        }, { e ->
                            context.addError(getName(), e)
                        })
                    }
                }
            }, { e ->
                context.addError(getName(), e)
            })

        }
        return retSet
    }


    private fun evaluateApplicationPaths(psiElement: PsiElement): Set<String?> {

        val appPathAnnotationClass = project.service<PsiPointers>().getPsiClass(project, applicationPathAnnotationClassFqn)

        val appPaths: MutableSet<String?> = HashSet()

        appPathAnnotationClass?.let {
            // check for ApplicationPath in context of module
            val module = ModuleUtilCore.findModuleForPsiElement(psiElement)
            if (module != null) {
                val appPathPsiClasses = AnnotatedElementsSearch.searchPsiClasses(appPathAnnotationClass, GlobalSearchScope.moduleScope(module))
                appPathPsiClasses.forEach { appPathClass ->
                    val appPathAnnotation = appPathClass.getAnnotation(applicationPathAnnotationClassFqn)
                    appPathAnnotation?.let {
                        val appPathValue = JavaLanguageUtils.getPsiAnnotationAttributeValue(it, "value")
                        appPaths.add(appPathValue)
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

        // value for example : 'epHTTP:HTTP GET /books/get'
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
