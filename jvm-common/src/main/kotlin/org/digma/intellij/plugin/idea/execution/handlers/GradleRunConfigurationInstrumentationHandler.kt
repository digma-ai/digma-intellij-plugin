package org.digma.intellij.plugin.idea.execution.handlers

import org.digma.intellij.plugin.execution.RunConfigurationInstrumentationService
import org.digma.intellij.plugin.idea.externalsystem.findGradleRunConfigurationInstrumentationService

class GradleRunConfigurationInstrumentationHandler : BaseRunConfigurationInstrumentationHandler() {

    private val gradleInstrumentationService: RunConfigurationInstrumentationService? = findGradleRunConfigurationInstrumentationService()

    override fun getService(): RunConfigurationInstrumentationService? {
        return gradleInstrumentationService
    }

}