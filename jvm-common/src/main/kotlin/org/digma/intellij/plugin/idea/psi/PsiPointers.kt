package org.digma.intellij.plugin.idea.psi

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.search.GlobalSearchScope
import org.digma.intellij.plugin.SPAN_BUILDER_FQN
import org.digma.intellij.plugin.WITH_SPAN_ANNOTATION_FQN
import org.digma.intellij.plugin.idea.psi.discovery.MicrometerTracingFramework
import org.digma.intellij.plugin.psi.runInReadAccessInSmartModeIgnorePCE
import org.digma.intellij.plugin.psi.runInReadAccessWithResult

@Service(Service.Level.PROJECT)
class PsiPointers(val project: Project) {

    private var withSpanAnnotationClassPointer: SmartPsiElementPointer<PsiClass>? = null
    private var traceBuilderPsiClassPointer: SmartPsiElementPointer<PsiClass>? = null
    private var startSpanPsiMethodPointer: SmartPsiElementPointer<PsiMethod>? = null
    private var micrometerObservedAnnotationClassPointer: SmartPsiElementPointer<PsiClass>? = null

    fun getOtelWithSpanAnnotationPsiClass(project: Project): PsiClass? {
        if (withSpanAnnotationClassPointer == null) {
            withSpanAnnotationClassPointer = runInReadAccessInSmartModeIgnorePCE(project) {
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


    fun getOtelTracerBuilderPsiClass(project: Project): PsiClass? {
        if (traceBuilderPsiClassPointer == null) {
            traceBuilderPsiClassPointer = runInReadAccessInSmartModeIgnorePCE(project) {
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

    fun getOtelStartSpanMethod(project: Project, builderClass: PsiClass): PsiMethod? {
        if (startSpanPsiMethodPointer == null) {
            startSpanPsiMethodPointer = runInReadAccessInSmartModeIgnorePCE(project) {
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


    fun getMicrometerObservedAnnotationPsiClass(project: Project): PsiClass? {
        if (micrometerObservedAnnotationClassPointer == null) {
            micrometerObservedAnnotationClassPointer = runInReadAccessInSmartModeIgnorePCE(project) {
                val observedClass = JavaPsiFacade.getInstance(project)
                    .findClass(MicrometerTracingFramework.OBSERVED_FQN, GlobalSearchScope.allScope(project))
                observedClass?.let {
                    SmartPointerManager.getInstance(project).createSmartPsiElementPointer(it)
                }
            }
        }

        return runInReadAccessWithResult {
            micrometerObservedAnnotationClassPointer?.element
        }
    }


}