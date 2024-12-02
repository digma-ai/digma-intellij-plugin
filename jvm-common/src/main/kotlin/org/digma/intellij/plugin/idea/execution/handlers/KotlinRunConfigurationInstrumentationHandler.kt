package org.digma.intellij.plugin.idea.execution.handlers

import org.digma.intellij.plugin.execution.RunConfigurationInstrumentationService
import org.digma.intellij.plugin.externalsystem.findKotlinRunConfigurationInstrumentationService

class KotlinRunConfigurationInstrumentationHandler : BaseRunConfigurationInstrumentationHandler() {

    private val kotlinInstrumentationService: RunConfigurationInstrumentationService? = findKotlinRunConfigurationInstrumentationService()

    override fun getService(): RunConfigurationInstrumentationService? {
        return kotlinInstrumentationService
    }


}