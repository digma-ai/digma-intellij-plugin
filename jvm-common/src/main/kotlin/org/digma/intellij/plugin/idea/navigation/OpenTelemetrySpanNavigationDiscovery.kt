package org.digma.intellij.plugin.idea.navigation

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.SPAN_BUILDER_FQN
import org.digma.intellij.plugin.WITH_SPAN_ANNOTATION_FQN
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.common.runInReadAccessInSmartModeWithResultAndRetryIgnorePCE
import org.digma.intellij.plugin.common.runInReadAccessWithResult
import org.digma.intellij.plugin.common.runInReadAccessWithRetryIgnorePCE
import org.digma.intellij.plugin.idea.navigation.model.NavigationProcessContext
import org.digma.intellij.plugin.idea.navigation.model.SpanLocation
import org.digma.intellij.plugin.idea.psi.PsiPointers
import org.digma.intellij.plugin.idea.psi.findAnnotatedMethods
import org.digma.intellij.plugin.idea.psi.findMethodReferences
import org.digma.intellij.plugin.idea.psi.java.JavaSpanDiscoveryUtils
import org.digma.intellij.plugin.model.discovery.SpanInfo
import java.util.Objects

class OpenTelemetrySpanNavigationDiscovery(private val project: Project) : SpanNavigationDiscoveryProvider {

    private val logger = Logger.getInstance(this::class.java)

    override fun getName(): String {
        return "OpenTelemetry"
    }


    override fun discover(context: NavigationProcessContext): Map<String, SpanLocation> {
        val spanLocations = mutableMapOf<String, SpanLocation>()

        val withSpanSpans: Map<String, SpanLocation> =
            executeCatchingWithResultWithRetry(context, getName(), 50, 5) {
                buildWithSpanAnnotation(context)
            } ?: mapOf()


        val startSpanSpans: Map<String, SpanLocation> =
            executeCatchingWithResultWithRetry(context, getName(), 50, 5) {
                buildStartSpan(context)
            } ?: mapOf()

        spanLocations.putAll(withSpanSpans)
        spanLocations.putAll(startSpanSpans)

        return spanLocations
    }


    //the search scope is lazily created. so it will be created inside a read action
    private fun buildWithSpanAnnotation(context: NavigationProcessContext): Map<String, SpanLocation> {

        if (!isProjectValid(project)) {
            return mapOf()
        }

        val psiPointers = project.service<PsiPointers>()

        val annotatedMethods =
            executeCatchingWithResultWithRetry(context, getName(), 50, 5) {
                psiPointers.getPsiClassPointer(project, WITH_SPAN_ANNOTATION_FQN)?.let { withSpanClassPointer ->
                    findAnnotatedMethods(project, withSpanClassPointer, context.searchScope)
                }
            } ?: return mapOf()


        if (annotatedMethods.isEmpty()) {
            return mapOf()
        }

        context.indicator.checkCanceled()

        val spanLocations = mutableMapOf<String, SpanLocation>()

        annotatedMethods.forEach { methodPointer ->

            executeCatchingWithRetry(context, getName(), 50, 5) {

                val element = runInReadAccessWithResult { methodPointer.element }

                element?.let { psiMethod ->

                    val spanInfos = runInReadAccessInSmartModeWithResultAndRetryIgnorePCE(project) {
                        JavaSpanDiscoveryUtils.getSpanInfoFromWithSpanAnnotatedMethod(psiMethod)
                    }

                    spanInfos.forEach { spanInfo: SpanInfo ->
                        runInReadAccessWithRetryIgnorePCE {
                            val offset = psiMethod.textOffset
                            val location = SpanLocation(spanInfo.containingFileUri, offset)
                            spanLocations[spanInfo.id] = location
                        }
                    }
                }
            }

            //check cancel on each iteration
            context.indicator.checkCanceled()

        }

        return spanLocations
    }


    private fun buildStartSpan(context: NavigationProcessContext): Map<String, SpanLocation> {

        if (!isProjectValid(project)) {
            return mapOf()
        }

        val psiPointers = project.service<PsiPointers>()

        val startSpanReferences = executeCatchingWithResultWithRetry(context, getName(), 50, 5) {
            val tracerBuilderClassPointer = psiPointers.getPsiClassPointer(project, SPAN_BUILDER_FQN)
            tracerBuilderClassPointer?.let {
                val startSpanMethodPointer = psiPointers.getOtelStartSpanMethodPointer(project, tracerBuilderClassPointer)
                Objects.requireNonNull(startSpanMethodPointer, "startSpan method must be found in SpanBuilder class")
                startSpanMethodPointer?.let {
                    findMethodReferences(project, startSpanMethodPointer, context.searchScope)
                } ?: listOf()
            }
        } ?: return mapOf()


        if (startSpanReferences.isEmpty()) {
            return mapOf()
        }

        val spanLocations = mutableMapOf<String, SpanLocation>()

        startSpanReferences.forEach { reference ->

            executeCatchingWithRetry(context, getName(), 50, 5) {
                val spanInfo = runInReadAccessInSmartModeWithResultAndRetryIgnorePCE(project) {
                    JavaSpanDiscoveryUtils.getSpanInfoFromStartSpanMethodReference(project, reference)
                }
                spanInfo?.let {
                    runInReadAccessWithRetryIgnorePCE {
                        val lineNumber = reference.element.textOffset
                        val location = SpanLocation(spanInfo.containingFileUri, lineNumber)
                        spanLocations[spanInfo.id] = location
                    }
                }
            }

            //check cancel on each iteration
            context.indicator.checkCanceled()
        }

        return spanLocations
    }
}

