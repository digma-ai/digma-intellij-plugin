package org.digma.intellij.plugin.idea.psi.discovery.span

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.search.searches.MethodReferencesSearch
import org.digma.intellij.plugin.SPAN_BUILDER_FQN
import org.digma.intellij.plugin.WITH_SPAN_ANNOTATION_FQN
import org.digma.intellij.plugin.common.SearchScopeProvider
import org.digma.intellij.plugin.common.executeCatchingWithResultIgnorePCE
import org.digma.intellij.plugin.common.executeCatchingWithRetryIgnorePCE
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.common.runInReadAccessInSmartModeWithResultAndRetryIgnorePCE
import org.digma.intellij.plugin.idea.psi.PsiPointers
import org.digma.intellij.plugin.idea.psi.discovery.MicrometerTracingFramework
import org.digma.intellij.plugin.model.discovery.SpanInfo
import org.digma.intellij.plugin.psi.BuildDocumentInfoProcessContext
import org.digma.intellij.plugin.psi.PsiFileCachedValueWithUri
import org.digma.intellij.plugin.psi.PsiUtils
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.toUElementOfType

abstract class AbstractSpanDiscovery {

    protected val logger: Logger = Logger.getInstance(this::class.java)

    private val micrometerTracingFramework = MicrometerTracingFramework()


    fun discoverSpans(
        project: Project,
        psiFileCachedValue: PsiFileCachedValueWithUri,
        context: BuildDocumentInfoProcessContext,
    ): Collection<SpanInfo> {

        val psiFile = psiFileCachedValue.value ?: return listOf()
        if (!isProjectValid(project) || !PsiUtils.isValidPsiFile(psiFile)) {
            return listOf()
        }

        val spanInfos = mutableListOf<SpanInfo>()

        executeCatchingWithRetryIgnorePCE({
            val withSpanSpans = discoverWithSpanAnnotationSpans(project, context) { GlobalSearchScope.fileScope(psiFile) }
            withSpanSpans?.let {
                spanInfos.addAll(it)
            }
        }, { e ->
            context.addError("discoverWithSpan", e)
        })


        executeCatchingWithRetryIgnorePCE({
            val startSpanSpans = discoverStartSpanMethodCallSpanDiscovery(project, context) { GlobalSearchScope.fileScope(psiFile) }
            startSpanSpans?.let {
                spanInfos.addAll(it)
            }
        }, { e ->
            context.addError("discoverStartSpan", e)
        })


        executeCatchingWithRetryIgnorePCE({
            val micrometerSpans = micrometerTracingFramework.discoverSpans(project, psiFileCachedValue, context)
            micrometerSpans.let {
                spanInfos.addAll(it)
            }
        }, { e ->
            context.addError("discoverMicrometerSpans", e)
        })


        return spanInfos
    }


    private fun discoverWithSpanAnnotationSpans(
        project: Project,
        context: BuildDocumentInfoProcessContext,
        searchScope: SearchScopeProvider,
    ): Collection<SpanInfo>? {

        val psiPointers = project.service<PsiPointers>()

        return psiPointers.getPsiClass(project, WITH_SPAN_ANNOTATION_FQN)?.let {

            val spanInfos = mutableListOf<SpanInfo>()

            val annotatedMethods: List<UMethod>? = psiPointers.getPsiClassPointer(project, WITH_SPAN_ANNOTATION_FQN)?.let { withSpanClassPointer ->
                findAnnotatedMethods(project, withSpanClassPointer, searchScope)
            }


            annotatedMethods?.forEach {

                //catch exceptions for each annotated method and skip it
                val methodSpans: List<SpanInfo> =
                    executeCatchingWithResultIgnorePCE({
                        runInReadAccessInSmartModeWithResultAndRetryIgnorePCE(project) {
                            findSpanInfosFromWithSpanAnnotatedMethod(it)
                        }
                    }, { e ->
                        context.addError("discoverWithSpanAnnotationSpans", e)
                        listOf()
                    })

                spanInfos.addAll(methodSpans)

            }

            return@let spanInfos
        }

    }

    //this method is open to let JavaSpanDiscovery override it and run the java implementation.
    //todo: refactor to have the same code for java and kotlin
    open fun findSpanInfosFromWithSpanAnnotatedMethod(uMethod: UMethod): List<SpanInfo> {
        return getSpanInfoFromWithSpanAnnotatedMethod(uMethod)
    }


    private fun findAnnotatedMethods(
        project: Project,
        annotationClassPointer: SmartPsiElementPointer<PsiClass>,
        searchScope: SearchScopeProvider,
    ): List<UMethod> {
        //todo: different search for java/kotlin, for kotlin use KotlinAnnotatedElementsSearcher or KotlinAnnotationsIndex
        return runInReadAccessInSmartModeWithResultAndRetryIgnorePCE(project) {
            annotationClassPointer.element?.let { annotationClass ->
                val psiMethods = AnnotatedElementsSearch.searchPsiMethods(annotationClass, searchScope.get())
                psiMethods.findAll().mapNotNull { psiMethod: PsiMethod -> psiMethod.toUElementOfType<UMethod>() }
            }
        } ?: listOf()
    }


    private fun discoverStartSpanMethodCallSpanDiscovery(
        project: Project,
        context: BuildDocumentInfoProcessContext,
        searchScope: SearchScopeProvider,
    ): Collection<SpanInfo>? {

        val psiPointers = project.service<PsiPointers>()

        return psiPointers.getPsiClassPointer(project, SPAN_BUILDER_FQN)?.let { tracerBuilderClassPointer ->

            val spanInfos = mutableListOf<SpanInfo>()

            val startSpanReferences: Collection<UReferenceExpression>? =
                psiPointers.getOtelStartSpanMethodPointer(project, tracerBuilderClassPointer)?.let { startSpanMethodPointer ->
                    findStartSpanMethodReferences(project, startSpanMethodPointer, searchScope)
                }

            startSpanReferences?.forEach { uReference ->

                //catch exceptions for each method reference and skip it
                val spanInfo: SpanInfo? =
                    executeCatchingWithResultIgnorePCE({
                        runInReadAccessInSmartModeWithResultAndRetryIgnorePCE(project) {
                            findSpanInfoFromStartSpanMethodReference(project, uReference)
                        }
                    }, { e ->
                        context.addError("discoverStartSpanMethodCallSpanDiscovery", e)
                        null
                    })

                spanInfo?.let { span ->
                    spanInfos.add(span)
                }

            }

            return spanInfos

        }
    }


    //this method is open to let JavaSpanDiscovery override it and run the java implementation.
    //todo: refactor to have the same code for java and kotlin
    open fun findSpanInfoFromStartSpanMethodReference(project: Project, uReferenceExpression: UReferenceExpression): SpanInfo? {
        return getSpanInfoFromStartSpanMethodReference(project, uReferenceExpression)
    }


    private fun findStartSpanMethodReferences(
        project: Project,
        startSpanMethodPointer: SmartPsiElementPointer<PsiMethod>,
        searchScope: SearchScopeProvider,
    ): Collection<UReferenceExpression>? {

        return runInReadAccessInSmartModeWithResultAndRetryIgnorePCE(project) {
            startSpanMethodPointer.element?.let { startSpanMethod ->
                val methodReferences = MethodReferencesSearch.search(startSpanMethod, searchScope.get(), true)
                methodReferences.findAll().mapNotNull { psiReference: PsiReference -> psiReference.element.toUElementOfType<UReferenceExpression>() }
            }
        }
    }

}