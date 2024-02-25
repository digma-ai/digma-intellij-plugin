package org.digma.intellij.plugin.idea.psi

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.externalSystem.autoimport.ProjectRefreshAction
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.instrumentation.InstrumentationProvider
import org.digma.intellij.plugin.instrumentation.MethodObservabilityInfo

abstract class AbstractJvmInstrumentationProvider(private val project: Project, private val languageService: AbstractJvmLanguageService) :
    InstrumentationProvider {

    override fun buildMethodObservabilityInfo(methodId: String): MethodObservabilityInfo {
        return languageService.canInstrumentMethod(methodId)
    }

    override fun addObservabilityDependency(methodId: String) {
        try {

            EDT.ensureEDT {
                try {
                    WriteAction.run<RuntimeException> {
                        languageService.addDependencyToOtelLib(methodId)
                        ProjectRefreshAction.refreshProject(project)
                    }
                } catch (e: Throwable) {
                    ErrorReporter.getInstance().reportError(project, "AbstractJvmInstrumentationProvider.addObservabilityDependency", e)
                }
            }
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "AbstractJvmInstrumentationProvider.addObservabilityDependency", e)
        }
    }

    override fun addObservability(methodId: String) {
        try {
            EDT.ensureEDT {
                try {
                    WriteAction.run<RuntimeException> {
                        val observabilityInfo = languageService.canInstrumentMethod(methodId)
                        if (!observabilityInfo.hasAnnotation) {
                            languageService.instrumentMethod(observabilityInfo)
                            ProjectRefreshAction.refreshProject(project)
                        }
                    }
                } catch (e: Throwable) {
                    ErrorReporter.getInstance().reportError(project, "AbstractJvmInstrumentationProvider.addObservability", e)
                }
            }
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "AbstractJvmInstrumentationProvider.addObservability", e)
        }
    }
}