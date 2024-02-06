package org.digma.intellij.plugin.idea.navigation

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.common.runInReadAccessInSmartModeWithResultAndRetryIgnorePCE
import org.digma.intellij.plugin.common.runInReadAccessWithRetryIgnorePCE
import org.digma.intellij.plugin.idea.navigation.model.NavigationProcessContext
import org.digma.intellij.plugin.idea.navigation.model.SpanLocation
import org.digma.intellij.plugin.idea.psi.PsiPointers
import org.digma.intellij.plugin.idea.psi.discovery.MicrometerTracingFramework
import org.digma.intellij.plugin.idea.psi.findAnnotatedMethods

class MicrometerSpanNavigationDiscovery(private val project: Project) : SpanNavigationDiscoveryProvider {

    private val logger = Logger.getInstance(this::class.java)

    override fun getName(): String {
        return "Micrometer"
    }

    override fun discover(context: NavigationProcessContext): Map<String, SpanLocation> {
        val spanLocations = mutableMapOf<String, SpanLocation>()


        val observedSpans: Map<String, SpanLocation> =
            executeCatchingWithResultWithRetry(context, getName(), 50, 5) {
                buildObservedAnnotationSpans(context)
            } ?: mapOf()


        spanLocations.putAll(observedSpans)

        return spanLocations

    }


    private fun buildObservedAnnotationSpans(context: NavigationProcessContext): Map<String, SpanLocation> {

        if (!isProjectValid(project)) {
            return mapOf()
        }

        val psiPointers = project.service<PsiPointers>()

        val annotatedMethods =
            executeCatchingWithResultWithRetry(context, getName(), 50, 5) {
                val observedClass =
                    psiPointers.getPsiClass(project, MicrometerTracingFramework.OBSERVED_FQN) ?: return@executeCatchingWithResultWithRetry listOf()
                findAnnotatedMethods(project, observedClass, context.searchScope)
            } ?: return mapOf()

        if (annotatedMethods.isEmpty()) {
            return mapOf()
        }


        val micrometerTracingFramework = MicrometerTracingFramework()
        val spanLocations = mutableMapOf<String, SpanLocation>()

        annotatedMethods.forEach { methodPointer ->

            executeCatchingWithRetry(context, getName(), 50, 5) {
                methodPointer.element?.let { psiMethod ->

                    val spanInfo = runInReadAccessInSmartModeWithResultAndRetryIgnorePCE(project) {
                        micrometerTracingFramework.getSpanInfoFromObservedAnnotatedMethod(psiMethod)
                    }

                    spanInfo?.let {
                        runInReadAccessWithRetryIgnorePCE {
                            val offset = psiMethod.textOffset
                            val location = SpanLocation(it.containingFileUri, offset)
                            spanLocations[it.id] = location
                        }
                    }
                }
            }

            //check cancel on each iteration
            context.indicator.checkCanceled()
        }

        return spanLocations
    }
}