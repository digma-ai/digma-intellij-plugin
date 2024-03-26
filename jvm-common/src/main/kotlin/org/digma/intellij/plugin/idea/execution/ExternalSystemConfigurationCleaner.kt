package org.digma.intellij.plugin.idea.execution

import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration

class ExternalSystemConfigurationCleaner(configuration: RunConfigurationBase<*>) : ConfigurationCleaner(configuration) {


    //currently we need to clean only for gradle
    override fun cleanConfiguration() {

        val myConfiguration = configuration as ExternalSystemRunConfiguration

        if (myConfiguration.settings.env.containsKey(ORG_JAVA_TOOL_OPTIONS)) {
            val newEnv = mutableMapOf<String, String>()
            val orgJavaToolOptions = myConfiguration.settings.env[ORG_JAVA_TOOL_OPTIONS]
            newEnv.putAll(myConfiguration.settings.env)
            if (orgJavaToolOptions != null) {
                newEnv[JAVA_TOOL_OPTIONS] = orgJavaToolOptions
            }
            newEnv.remove(ORG_JAVA_TOOL_OPTIONS)
            myConfiguration.settings.env = newEnv
        } else if (myConfiguration.settings.env.containsKey(JAVA_TOOL_OPTIONS)) {
            val newEnv = mutableMapOf<String, String>()
            newEnv.putAll(myConfiguration.settings.env)
            newEnv.remove(JAVA_TOOL_OPTIONS)
            myConfiguration.settings.env = newEnv
        }

        cleanOtelResourceAttributes(myConfiguration)
    }


    private fun cleanOtelResourceAttributes(configuration: ExternalSystemRunConfiguration) {

        if (configuration.settings.env.containsKey(ORG_OTEL_RESOURCE_ATTRIBUTES)) {
            val newEnv = mutableMapOf<String, String>()
            val orgOtelResourceAttributes = configuration.settings.env[ORG_OTEL_RESOURCE_ATTRIBUTES]
            newEnv.putAll(configuration.settings.env)
            if (orgOtelResourceAttributes != null) {
                newEnv[OTEL_RESOURCE_ATTRIBUTES] = orgOtelResourceAttributes
            }
            newEnv.remove(ORG_OTEL_RESOURCE_ATTRIBUTES)
            configuration.settings.env = newEnv
        } else if (configuration.settings.env.containsKey(OTEL_RESOURCE_ATTRIBUTES)) {
            val newEnv = mutableMapOf<String, String>()
            newEnv.putAll(configuration.settings.env)
            newEnv.remove(OTEL_RESOURCE_ATTRIBUTES)
            configuration.settings.env = newEnv
        }
    }
}