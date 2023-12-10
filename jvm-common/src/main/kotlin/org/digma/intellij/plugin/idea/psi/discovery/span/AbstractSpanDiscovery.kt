package org.digma.intellij.plugin.idea.psi.discovery.span

import com.intellij.openapi.project.Project
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
import org.digma.intellij.plugin.idea.psi.SPAN_BUILDER_FQN
import org.digma.intellij.plugin.idea.psi.WITH_SPAN_ANNOTATION_FQN
import org.digma.intellij.plugin.idea.psi.findMethodInClass
import org.digma.intellij.plugin.model.discovery.SpanInfo
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.toUElementOfType

abstract class AbstractSpanDiscovery {


    private val psiPointers = PsiPointers()


    fun discoverSpans(project: Project, psiFile: PsiFile): Collection<SpanInfo> {

        val spanInfos = mutableListOf<SpanInfo>()

        val withSpanSpans = discoverWithSpanAnnotationSpans(project, GlobalSearchScope.fileScope(psiFile))
        withSpanSpans?.let {
            spanInfos.addAll(it)
        }


        val startSpanSpans = discoverStartSpanMethodCallSpanDiscovery(project, GlobalSearchScope.fileScope(psiFile))
        startSpanSpans?.let {
            spanInfos.addAll(it)
        }

        return spanInfos
    }


    private fun discoverWithSpanAnnotationSpans(project: Project, searchScope: GlobalSearchScope): Collection<SpanInfo>? {

        val withSpanAnnotationClass = psiPointers.getWithSpanAnnotationPsiClass(project)

        return withSpanAnnotationClass?.let { withSpanClass ->

            val spanInfos = mutableListOf<SpanInfo>()

            val annotatedMethods: List<UMethod> = findAnnotatedMethods(project, withSpanClass, searchScope)

            annotatedMethods.forEach {

                val methodSpans: List<SpanInfo> = getSpanInfoFromWithSpanAnnotatedMethod(it)
                spanInfos.addAll(methodSpans)

            }

            return@let spanInfos
        }

    }


    private fun findAnnotatedMethods(project: Project, withSpanClass: PsiClass, searchScope: GlobalSearchScope): List<UMethod> {
        val psiMethods = AnnotatedElementsSearch.searchPsiMethods(withSpanClass, searchScope)
        return psiMethods.findAll().map { psiMethod: PsiMethod -> psiMethod.toUElementOfType<UMethod>()!! }
    }


    private fun discoverStartSpanMethodCallSpanDiscovery(project: Project, searchScope: GlobalSearchScope): Collection<SpanInfo>? {

        val tracerBuilderClass = psiPointers.getTracerBuilderPsiClass(project)

        return tracerBuilderClass?.let { builderClass ->

            val startSpanMethod: PsiMethod? =
                findMethodInClass(builderClass, "startSpan") { psiMethod: PsiMethod -> psiMethod.parameters.isEmpty() }

            return startSpanMethod?.let {

                val spanInfos = mutableListOf<SpanInfo>()

                val startSpanReferences: Collection<UReferenceExpression> = findStartSpanMethodReferences(project, it, searchScope)

                startSpanReferences.forEach { uReference ->
                    val spanInfo: SpanInfo? = getSpanInfoFromStartSpanMethodReference(project, uReference)
                    spanInfo?.let {
                        spanInfos.add(it)
                    }

                }

                return spanInfos

            }
        }

    }

    private fun findStartSpanMethodReferences(
        project: Project,
        startSpanMethod: PsiMethod,
        searchScope: GlobalSearchScope,
    ): Collection<UReferenceExpression> {

        val methodReferences = MethodReferencesSearch.search(startSpanMethod, searchScope, true);
        //todo : maybe filter UReferenceExpression
        return methodReferences.findAll().map { psiReference: PsiReference -> psiReference.element.toUElementOfType<UReferenceExpression>()!! }
    }


    //holder for psi elements that are necessary for the lifetime of the project.
    //lazy initialized so that search will happen with read action scope
    class PsiPointers {

        private var withSpanAnnotationClass: SmartPsiElementPointer<PsiClass>? = null
        private var traceBuilderPsiClass: SmartPsiElementPointer<PsiClass>? = null

        fun getWithSpanAnnotationPsiClass(project: Project): PsiClass? {
            if (withSpanAnnotationClass == null) {
                val withSpanClass = JavaPsiFacade.getInstance(project)
                    .findClass(WITH_SPAN_ANNOTATION_FQN, GlobalSearchScope.allScope(project))
                withSpanClass?.let {
                    withSpanAnnotationClass = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(it)
                }
            }

            return withSpanAnnotationClass?.element
        }


        fun getTracerBuilderPsiClass(project: Project): PsiClass? {
            if (traceBuilderPsiClass == null) {
                val tracerBuilderClass = JavaPsiFacade.getInstance(project)
                    .findClass(SPAN_BUILDER_FQN, GlobalSearchScope.allScope(project))
                tracerBuilderClass?.let {
                    traceBuilderPsiClass = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(it)
                }
            }

            return traceBuilderPsiClass?.element
        }
    }
}