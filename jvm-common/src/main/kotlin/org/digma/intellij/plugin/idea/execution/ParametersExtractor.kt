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

    private fun extractOtelServiceNameFromExternalSystem(configuration: RunConfiguration): String? {

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

    private fun extractOtelServiceNameFromVmOptions(params: SimpleProgramParameters): String? {
        if (params is SimpleJavaParameters) {
            return params.vmParametersList?.getPropertyValue(OTEL_SERVICE_NAME_PROP_NAME)
        }
        return null
    }

    private fun extractOtelServiceNameFromParamsEnv(params: SimpleProgramParameters): String? {
        return params.env[OTEL_SERVICE_NAME_ENV_VAR_NAME]
    }


    open fun hasDigmaEnvironmentIdAttribute(): Boolean {
        if (configuration is ExternalSystemRunConfiguration &&
            configuration.settings.env[OTEL_RESOURCE_ATTRIBUTES]?.contains("$DIGMA_ENVIRONMENT_ID_RESOURCE_ATTRIBUTE=") == true
        ) {
            return true
        }

        return params.env[OTEL_RESOURCE_ATTRIBUTES]?.contains("$DIGMA_ENVIRONMENT_ID_RESOURCE_ATTRIBUTE=") ?: false
    }

    open fun hasDigmaEnvironmentAttribute(): Boolean {
        if (configuration is ExternalSystemRunConfiguration &&
            configuration.settings.env[OTEL_RESOURCE_ATTRIBUTES]?.contains("$DIGMA_ENVIRONMENT_NAME_RESOURCE_ATTRIBUTE=") == true
        ) {
            return true
        }

        return params.env[OTEL_RESOURCE_ATTRIBUTES]?.contains("$DIGMA_ENVIRONMENT_NAME_RESOURCE_ATTRIBUTE=") ?: false
    }


    fun getDigmaObservability(): DigmaObservabilityType? {
        return extractEnvValue(DIGMA_OBSERVABILITY)?.takeIf { it.isNotBlank() }?.let {
            DigmaObservabilityType.valueOf(it)
        } ?: if (isEnvExists(DIGMA_OBSERVABILITY)) DigmaObservabilityType.app else null
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

    fun isEnvExists(envKeyName: String): Boolean {

        if (configuration is ExternalSystemRunConfiguration) {
            return configuration.settings.env.containsKey(envKeyName)
        }

        return params.env.containsKey(envKeyName)

    }


}