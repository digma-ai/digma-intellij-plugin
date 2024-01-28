package org.digma.intellij.plugin.idea.psi.discovery.span

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.search.searches.MethodReferencesSearch
import org.digma.intellij.plugin.SPAN_BUILDER_FQN
import org.digma.intellij.plugin.WITH_SPAN_ANNOTATION_FQN
import org.digma.intellij.plugin.common.SearchScopeProvider
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.psi.discovery.MicrometerTracingFramework
import org.digma.intellij.plugin.idea.psi.executeCatching
import org.digma.intellij.plugin.idea.psi.findMethodInClass
import org.digma.intellij.plugin.idea.psi.runInReadAccessInSmartModeWithResultAndRetry
import org.digma.intellij.plugin.idea.psi.runInReadAccessWithResult
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.SpanInfo
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.toUElementOfType

abstract class AbstractSpanDiscovery {

    protected val logger: Logger = Logger.getInstance(this::class.java)

    private val psiPointers = PsiPointers()

    private val micrometerTracingFramework = MicrometerTracingFramework()


    fun discoverSpans(project: Project, psiFile: PsiFile): Collection<SpanInfo> {

        if (project.isDisposed || !psiFile.isValid) {
            return listOf()
        }

        val spanInfos = mutableListOf<SpanInfo>()

        executeCatching({
            val withSpanSpans = discoverWithSpanAnnotationSpans(project) { GlobalSearchScope.fileScope(psiFile) }
            withSpanSpans?.let {
                spanInfos.addAll(it)
            }
        }) { e ->
            Log.warnWithException(logger, project, e, "error in span discovery {}", e)
            ErrorReporter.getInstance().reportError("${this::class.java.simpleName}.discoverSpans", e)
        }


        executeCatching({
            val startSpanSpans = discoverStartSpanMethodCallSpanDiscovery(project) { GlobalSearchScope.fileScope(psiFile) }
            startSpanSpans?.let {
                spanInfos.addAll(it)
            }
        }) { e ->
            Log.warnWithException(logger, project, e, "error in span discovery {}", e)
            ErrorReporter.getInstance().reportError("${this::class.java.simpleName}.discoverSpans", e)
        }


        executeCatching({
            val micrometerSpans = runInReadAccessInSmartModeWithResultAndRetry(project) {
                micrometerTracingFramework.discoverSpans(project, psiFile)
            }
            micrometerSpans.let {
                spanInfos.addAll(it)
            }
        }) { e ->
            Log.warnWithException(logger, project, e, "error in span discovery {}", e)
            ErrorReporter.getInstance().reportError("${this::class.java.simpleName}.discoverSpans", e)
        }


        return spanInfos
    }


    private fun discoverWithSpanAnnotationSpans(project: Project, searchScope: SearchScopeProvider): Collection<SpanInfo>? {

        val withSpanAnnotationClass = psiPointers.getWithSpanAnnotationPsiClass(project)

        return withSpanAnnotationClass?.let { withSpanClass ->

            val spanInfos = mutableListOf<SpanInfo>()

            val annotatedMethods: List<UMethod> = findAnnotatedMethods(project, withSpanClass, searchScope)

            annotatedMethods.forEach {

                //catch exceptions for each annotated method and skip it
                val methodSpans: List<SpanInfo> =
                    executeCatching(Computable {
                        runInReadAccessInSmartModeWithResultAndRetry(project) {
                            findSpanInfosFromWithSpanAnnotatedMethod(it)
                        }
                    }) { e ->
                        Log.warnWithException(logger, project, e, "error in span discovery {}", e)
                        ErrorReporter.getInstance().reportError("${this::class.java.simpleName}.discoverWithSpanAnnotationSpans", e)
                        listOf()
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


    private fun findAnnotatedMethods(project: Project, annotationClass: PsiClass, searchScope: SearchScopeProvider): List<UMethod> {
        //todo: different search for java/kotlin, for kotlin use KotlinAnnotatedElementsSearcher or KotlinAnnotationsIndex
        return runInReadAccessInSmartModeWithResultAndRetry(project) {
            val psiMethods = AnnotatedElementsSearch.searchPsiMethods(annotationClass, searchScope.get())
            psiMethods.findAll().mapNotNull { psiMethod: PsiMethod -> psiMethod.toUElementOfType<UMethod>() }
        }
    }


    private fun discoverStartSpanMethodCallSpanDiscovery(project: Project, searchScope: SearchScopeProvider): Collection<SpanInfo>? {

        val tracerBuilderClass = psiPointers.getTracerBuilderPsiClass(project)

        return tracerBuilderClass?.let { builderClass ->

            val startSpanMethod = psiPointers.getStartSpanMethod(project, builderClass)


            return startSpanMethod?.let { method ->

                val spanInfos = mutableListOf<SpanInfo>()

                val startSpanReferences: Collection<UReferenceExpression> = findStartSpanMethodReferences(project, method, searchScope)

                startSpanReferences.forEach { uReference ->

                    //catch exceptions for each method reference and skip it
                    val spanInfo: SpanInfo? =
                        executeCatching(Computable {
                            runInReadAccessInSmartModeWithResultAndRetry(project) {
                                findSpanInfoFromStartSpanMethodReference(project, uReference)
                            }
                        }) { e ->
                            Log.warnWithException(logger, project, e, "error in span discovery {}", e)
                            ErrorReporter.getInstance().reportError("${this::class.java.simpleName}.discoverStartSpanMethodCallSpanDiscovery", e)
                            null
                        }

                    spanInfo?.let { span ->
                        spanInfos.add(span)
                    }

                }

                return spanInfos

            }
        }
    }


    //this method is open to let JavaSpanDiscovery override it and run the java implementation.
    //todo: refactor to have the same code for java and kotlin
    open fun findSpanInfoFromStartSpanMethodReference(project: Project, uReferenceExpression: UReferenceExpression): SpanInfo? {
        return getSpanInfoFromStartSpanMethodReference(project, uReferenceExpression)
    }


    private fun findStartSpanMethodReferences(
        project: Project,
        startSpanMethod: PsiMethod,
        searchScope: SearchScopeProvider,
    ): Collection<UReferenceExpression> {

        return runInReadAccessInSmartModeWithResultAndRetry(project) {
            val methodReferences = MethodReferencesSearch.search(startSpanMethod, searchScope.get(), true)
            methodReferences.findAll().mapNotNull { psiReference: PsiReference -> psiReference.element.toUElementOfType<UReferenceExpression>() }
        }

    }


    //holder for psi elements that are necessary for the lifetime of the project.
    //lazy initialized so that search will happen with read action scope
    private class PsiPointers {

        private var withSpanAnnotationClassPointer: SmartPsiElementPointer<PsiClass>? = null
        private var traceBuilderPsiClassPointer: SmartPsiElementPointer<PsiClass>? = null
        private var startSpanPsiMethodPointer: SmartPsiElementPointer<PsiMethod>? = null

        fun getWithSpanAnnotationPsiClass(project: Project): PsiClass? {
            if (withSpanAnnotationClassPointer == null) {
                withSpanAnnotationClassPointer = runInReadAccessInSmartModeWithResultAndRetry(project) {
                    val withSpanClass = JavaPsiFacade.getInstance(project)
                        .findClass(WITH_SPAN_ANNOTATION_FQN, GlobalSearchScope.allScope(project))
                    withSpanClass?.let {
                        SmartPointerManager.getInstance(project).createSmartPsiElementPointer(it)
                    }
                }
            }

            return runInReadAccessWithResult {
                withSpanAnnotationClassPointer?.element
            }
        }


        fun getTracerBuilderPsiClass(project: Project): PsiClass? {
            if (traceBuilderPsiClassPointer == null) {
                traceBuilderPsiClassPointer = runInReadAccessInSmartModeWithResultAndRetry(project) {
                    val tracerBuilderClass = JavaPsiFacade.getInstance(project)
                        .findClass(SPAN_BUILDER_FQN, GlobalSearchScope.allScope(project))
                    tracerBuilderClass?.let {
                        SmartPointerManager.getInstance(project).createSmartPsiElementPointer(it)
                    }
                }
            }

            return runInReadAccessWithResult {
                traceBuilderPsiClassPointer?.element
            }
        }

        fun getStartSpanMethod(project: Project, builderClass: PsiClass): PsiMethod? {
            if (startSpanPsiMethodPointer == null) {
                startSpanPsiMethodPointer = runInReadAccessInSmartModeWithResultAndRetry(project) {
                    val startSpanMethod: PsiMethod? = findMethodInClass(builderClass, "startSpan") { psiMethod: PsiMethod ->
                        @Suppress("UnstableApiUsage")
                        psiMethod.parameters.isEmpty()
                    }
                    startSpanMethod?.let {
                        SmartPointerManager.getInstance(project).createSmartPsiElementPointer(it)
                    }
                }
            }

            return runInReadAccessWithResult {
                startSpanPsiMethodPointer?.element
            }
        }
    }
}