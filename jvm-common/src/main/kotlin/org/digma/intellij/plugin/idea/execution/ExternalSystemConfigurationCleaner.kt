package org.digma.intellij.plugin.idea.execution

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration

class ExternalSystemConfigurationCleaner(configuration: RunConfiguration) : ConfigurationCleaner(configuration) {

    //currently we need to clean only for gradle
    override fun cleanConfiguration() {

        if (configuration is ExternalSystemRunConfiguration) {
            val orgEnv = ExternalSystemConfigurationTempStorage.orgConfigurationEnvironmentVars[configuration]
            if (orgEnv != null) {
                configuration.settings.env = orgEnv
            }
        }
    }
}