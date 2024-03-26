package org.digma.intellij.plugin.idea.execution.handlers

import com.intellij.openapi.components.service
import org.digma.intellij.plugin.execution.RunConfigurationInstrumentationService
import org.digma.intellij.plugin.idea.execution.services.OpenLibertyRunConfigurationInstrumentationService

class OpenLibertyServerRunConfigurationInstrumentationHandler : BaseRunConfigurationInstrumentationHandler() {

    override fun getService(): RunConfigurationInstrumentationService {
        return service<OpenLibertyRunConfigurationInstrumentationService>()
    }

    override fun getOrder(): Int {
        return 200
    }

}