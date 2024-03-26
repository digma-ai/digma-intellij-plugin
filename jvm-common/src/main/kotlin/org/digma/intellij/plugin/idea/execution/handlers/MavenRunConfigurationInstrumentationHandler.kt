package org.digma.intellij.plugin.idea.execution.handlers

import org.digma.intellij.plugin.execution.RunConfigurationInstrumentationService
import org.digma.intellij.plugin.idea.execution.findMavenService

class MavenRunConfigurationInstrumentationHandler : BaseRunConfigurationInstrumentationHandler() {

    private val mavenInstrumentationService: RunConfigurationInstrumentationService? = findMavenService()

    override fun getService(): RunConfigurationInstrumentationService? {
        return mavenInstrumentationService
    }

    override fun getOrder(): Int {
        return 20
    }

}