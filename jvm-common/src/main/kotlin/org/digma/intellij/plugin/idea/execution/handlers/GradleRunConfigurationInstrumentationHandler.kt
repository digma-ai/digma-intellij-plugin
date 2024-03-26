package org.digma.intellij.plugin.idea.execution.handlers

import org.digma.intellij.plugin.execution.RunConfigurationInstrumentationService
import org.digma.intellij.plugin.idea.execution.findGradleService

class GradleRunConfigurationInstrumentationHandler : BaseRunConfigurationInstrumentationHandler() {

    private val gradleInstrumentationService: RunConfigurationInstrumentationService? = findGradleService()

    override fun getService(): RunConfigurationInstrumentationService? {
        return gradleInstrumentationService
    }

    override fun getOrder(): Int {
        return 10
    }
}