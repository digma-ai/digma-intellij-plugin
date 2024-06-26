package org.digma.intellij.plugin.idea.execution.handlers

import org.digma.intellij.plugin.execution.RunConfigurationInstrumentationService
import org.digma.intellij.plugin.externalsystem.findMavenRunConfigurationInstrumentationService

class MavenRunConfigurationInstrumentationHandler : BaseRunConfigurationInstrumentationHandler() {

    private val mavenInstrumentationService: RunConfigurationInstrumentationService? = findMavenRunConfigurationInstrumentationService()

    override fun getService(): RunConfigurationInstrumentationService? {
        return mavenInstrumentationService
    }

}