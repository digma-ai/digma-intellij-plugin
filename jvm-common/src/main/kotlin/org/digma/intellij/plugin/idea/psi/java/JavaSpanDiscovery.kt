@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.idea.psi.java

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.search.searches.MethodReferencesSearch
import org.digma.intellij.plugin.SPAN_BUILDER_FQN
import org.digma.intellij.plugin.WITH_SPAN_ANNOTATION_FQN
import org.digma.intellij.plugin.idea.psi.discovery.span.AbstractSpanDiscovery
import org.digma.intellij.plugin.model.discovery.SpanInfo
import java.util.Objects

class JavaSpanDiscovery : AbstractSpanDiscovery() {


    //todo: currently using existing code written for java only in JavaSpanDiscoveryUtils.
    // make common code for java and kotlin. use only AbstractSpanDiscovery.discoverSpans
    override fun discoverSpans(project: Project, psiFile: PsiFile): Collection<SpanInfo> {

        val spanInfos = mutableListOf<SpanInfo>()

        val withSpanSpans = withSpanAnnotationSpanDiscovery(project, psiFile)
        withSpanSpans.let {
            spanInfos.addAll(it)
        }

        val startSpanSpans = startSpanMethodCallSpanDiscovery(project, psiFile)
        startSpanSpans.let {
            spanInfos.addAll(it)
        }


        val micrometerSpans = micrometerTracingFramework.discoverSpans(project, psiFile)
        micrometerSpans.let {
            spanInfos.addAll(it)
        }

        return spanInfos
    }


    private fun withSpanAnnotationSpanDiscovery(project: Project, psiFile: PsiFile): Collection<SpanInfo> {

        val spanInfos = mutableListOf<SpanInfo>()

        val withSpanClass = JavaPsiFacade.getInstance(project).findClass(WITH_SPAN_ANNOTATION_FQN, GlobalSearchScope.allScope(project))
        //maybe the annotation is not in the classpath
        if (withSpanClass != null) {
            var psiMethods = AnnotatedElementsSearch.searchPsiMethods(withSpanClass, GlobalSearchScope.fileScope(psiFile))
            psiMethods = JavaSpanDiscoveryUtils.filterNonRelevantMethodsForSpanDiscovery(psiMethods)

            psiMethods.forEach {
                val spans = JavaSpanDiscoveryUtils.getSpanInfoFromWithSpanAnnotatedMethod(it)
                spanInfos.addAll(spans)
            }
        }

        return spanInfos
    }


    private fun startSpanMethodCallSpanDiscovery(project: Project, psiFile: PsiFile): Collection<SpanInfo> {

        val spanInfos = mutableListOf<SpanInfo>()

        val tracerBuilderClass = JavaPsiFacade.getInstance(project).findClass(SPAN_BUILDER_FQN, GlobalSearchScope.allScope(project))
        if (tracerBuilderClass != null) {
            val startSpanMethod =
                JavaLanguageUtils.findMethodInClass(tracerBuilderClass, "startSpan") { psiMethod: PsiMethod -> psiMethod.parameters.isEmpty() }
            Objects.requireNonNull(startSpanMethod, "startSpan method must be found in SpanBuilder class")

            var startSpanReferences = MethodReferencesSearch.search(startSpanMethod!!, GlobalSearchScope.fileScope(psiFile), true)
            //filter classes that we don't support,which should not happen but just in case. we don't support Annotations,Enums and Records.
            startSpanReferences = JavaSpanDiscoveryUtils.filterNonRelevantReferencesForSpanDiscovery(startSpanReferences)

            startSpanReferences.forEach { ref ->
                val spanInfo = JavaSpanDiscoveryUtils.getSpanInfoFromStartSpanMethodReference(project, ref)
                spanInfo?.let {
                    spanInfos.add(it)
                }

            }
        }

        return spanInfos
    }


}