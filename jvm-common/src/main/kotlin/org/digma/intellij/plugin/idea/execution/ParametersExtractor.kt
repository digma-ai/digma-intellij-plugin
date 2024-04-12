package org.digma.intellij.plugin.idea.execution

import com.intellij.execution.configurations.ParametersList
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleJavaParameters
import com.intellij.execution.configurations.SimpleProgramParameters
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration

open class ParametersExtractor(protected val configuration: RunConfiguration, protected val params: SimpleProgramParameters) {


    open fun isOtelServiceNameAlreadyDefined(): Boolean {
        return extractOtelServiceName() != null
    }


    open fun extractOtelServiceName(): String? {
        return extractOtelServiceNameFromParamsEnv(params) ?: extractOtelServiceNameFromVmOptions(params) ?: extractOtelServiceNameFromExternalSystem(
            configuration
        )
    }

    open fun extractOtelServiceNameFromExternalSystem(configuration: RunConfiguration): String? {

        return if (configuration is ExternalSystemRunConfiguration) {
            configuration.settings.env[OTEL_SERVICE_NAME_ENV_VAR_NAME]
                ?: configuration.settings.vmOptions?.let { vmOptions ->
                    val vmParList = ParametersList()
                    vmParList.addParametersString(vmOptions)
                    vmParList.getPropertyValue(OTEL_SERVICE_NAME_PROP_NAME)
                }
        } else {
            null
        }
    }

    open fun extractOtelServiceNameFromVmOptions(params: SimpleProgramParameters): String? {
        if (params is SimpleJavaParameters) {
            return params.vmParametersList?.getPropertyValue(OTEL_SERVICE_NAME_PROP_NAME)
        }
        return null
    }

    open fun extractOtelServiceNameFromParamsEnv(params: SimpleProgramParameters): String? {
        return params.env[OTEL_SERVICE_NAME_ENV_VAR_NAME]
    }


    open fun hasDigmaEnvironmentIdAttribute(configuration: RunConfiguration, params: SimpleProgramParameters): Boolean {
        if (configuration is ExternalSystemRunConfiguration &&
            configuration.settings.env[OTEL_RESOURCE_ATTRIBUTES]?.contains("$DIGMA_ENVIRONMENT_ID_RESOURCE_ATTRIBUTE=") == true
        ) {
            return true
        }

        return params.env[OTEL_RESOURCE_ATTRIBUTES]?.contains("$DIGMA_ENVIRONMENT_ID_RESOURCE_ATTRIBUTE=") ?: false
    }

    fun extractEnvValue(envKeyName: String): String? {

        if (configuration is ExternalSystemRunConfiguration &&
            configuration.settings.env[envKeyName] != null
        ) {
            return configuration.settings.env[envKeyName]
        }

        if (params.env[envKeyName] != null) {
            return params.env[envKeyName]
        }

        return null

    }


}