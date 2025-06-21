package org.digma.intellij.plugin.idea.discovery.span

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.search.searches.MethodReferencesSearch
import kotlinx.coroutines.ensureActive
import org.digma.intellij.plugin.common.SearchScopeProvider
import org.digma.intellij.plugin.idea.discovery.MicrometerTracingFramework
import org.digma.intellij.plugin.idea.discovery.PsiPointers
import org.digma.intellij.plugin.idea.discovery.SPAN_BUILDER_FQN
import org.digma.intellij.plugin.idea.discovery.WITH_SPAN_ANNOTATION_FQN
import org.digma.intellij.plugin.model.discovery.SpanInfo
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.toUElementOfType
import kotlin.coroutines.coroutineContext

abstract class AbstractSpanDiscovery {

    protected val logger: Logger = Logger.getInstance(this::class.java)

    private val micrometerTracingFramework = MicrometerTracingFramework()


    suspend fun discoverSpans(project: Project, psiFile: PsiFile): Collection<SpanInfo> {

        val spanInfos = mutableListOf<SpanInfo>()

        val withSpanSpans = discoverWithSpanAnnotationSpans(project) { GlobalSearchScope.fileScope(psiFile) }
        withSpanSpans?.let {
            spanInfos.addAll(it)
        }
        coroutineContext.ensureActive()

        val startSpanSpans = discoverStartSpanMethodCallSpanDiscovery(project) { GlobalSearchScope.fileScope(psiFile) }
        startSpanSpans?.let {
            spanInfos.addAll(it)
        }
        coroutineContext.ensureActive()


        //don't fail the discovery of micrometer failed
        val micrometerSpans = micrometerTracingFramework.discoverSpans(project, psiFile)
        micrometerSpans.let {
            spanInfos.addAll(it)
        }
        coroutineContext.ensureActive()

        return spanInfos
    }


    private suspend fun discoverWithSpanAnnotationSpans(
        project: Project,
        searchScope: SearchScopeProvider,
    ): Collection<SpanInfo>? {

        val psiPointers = project.service<PsiPointers>()

        return psiPointers.getPsiClass(project, WITH_SPAN_ANNOTATION_FQN)?.let {
            coroutineContext.ensureActive()
            val spanInfos = mutableListOf<SpanInfo>()

            val annotatedMethods: List<UMethod>? = psiPointers.getPsiClassPointer(project, WITH_SPAN_ANNOTATION_FQN)?.let { withSpanClassPointer ->
                findAnnotatedMethods(project, withSpanClassPointer, searchScope)
            }

            coroutineContext.ensureActive()

            annotatedMethods?.forEach {
                coroutineContext.ensureActive()
                val methodSpans: List<SpanInfo> =
                    smartReadAction(project) {
                        findSpanInfosFromWithSpanAnnotatedMethod(it)
                    }

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


    private suspend fun findAnnotatedMethods(
        project: Project,
        annotationClassPointer: SmartPsiElementPointer<PsiClass>,
        searchScope: SearchScopeProvider,
    ): List<UMethod> {
        //todo: different search for java/kotlin, for kotlin use KotlinAnnotatedElementsSearcher or KotlinAnnotationsIndex
        return smartReadAction(project) {
            annotationClassPointer.element?.let { annotationClass ->
                val psiMethods = AnnotatedElementsSearch.searchPsiMethods(annotationClass, searchScope.get())
                psiMethods.findAll().mapNotNull { psiMethod: PsiMethod -> psiMethod.toUElementOfType<UMethod>() }
            }
        } ?: listOf()
    }


    private suspend fun discoverStartSpanMethodCallSpanDiscovery(project: Project, searchScope: SearchScopeProvider): Collection<SpanInfo>? {

        val psiPointers = project.service<PsiPointers>()

        return psiPointers.getPsiClassPointer(project, SPAN_BUILDER_FQN)?.let { tracerBuilderClassPointer ->
            coroutineContext.ensureActive()
            val spanInfos = mutableListOf<SpanInfo>()

            val startSpanReferences: Collection<UReferenceExpression>? =
                psiPointers.getOtelStartSpanMethodPointer(project, tracerBuilderClassPointer)?.let { startSpanMethodPointer ->
                    findStartSpanMethodReferences(project, startSpanMethodPointer, searchScope)
                }

            coroutineContext.ensureActive()

            startSpanReferences?.forEach { uReference ->
                coroutineContext.ensureActive()
                //catch exceptions for each method reference and skip it
                val spanInfo: SpanInfo? =
                    smartReadAction(project) {
                        findSpanInfoFromStartSpanMethodReference(project, uReference)
                    }
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


    private suspend fun findStartSpanMethodReferences(
        project: Project,
        startSpanMethodPointer: SmartPsiElementPointer<PsiMethod>,
        searchScope: SearchScopeProvider,
    ): Collection<UReferenceExpression>? {

        coroutineContext.ensureActive()
        val methodReferences = smartReadAction(project) {
            startSpanMethodPointer.element?.let { startSpanMethod ->
                MethodReferencesSearch.search(startSpanMethod, searchScope.get(), true)
            }
        }
        coroutineContext.ensureActive()
        return methodReferences?.let {
            readAction {
                it.findAll().mapNotNull { psiReference: PsiReference -> psiReference.element.toUElementOfType<UReferenceExpression>() }
            }
        }
    }

}