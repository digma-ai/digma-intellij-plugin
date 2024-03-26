package org.digma.intellij.plugin.idea.execution.handlers

import com.intellij.openapi.components.service
import org.digma.intellij.plugin.execution.RunConfigurationInstrumentationService
import org.digma.intellij.plugin.idea.execution.services.JavaEEAppServerRunConfigurationInstrumentationService

class JavaEEAppServerRunConfigurationInstrumentationHandler : BaseRunConfigurationInstrumentationHandler() {

    override fun getService(): RunConfigurationInstrumentationService {
        return service<JavaEEAppServerRunConfigurationInstrumentationService>()
    }

    override fun getOrder(): Int {
        return 100
    }

}