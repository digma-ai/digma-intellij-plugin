package org.digma.intellij.plugin.idea.execution

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleProgramParameters
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import org.digma.intellij.plugin.idea.execution.flavor.InstrumentationFlavorType


class ExternalSystemJavaParametersMerger(
    configuration: RunConfiguration,
    params: SimpleProgramParameters,
    parametersExtractor: ParametersExtractor
) : JavaParametersMerger(configuration, params, parametersExtractor) {

    override fun mergeJavaToolOptionsAndOtelResourceAttributes(
        instrumentationFlavorType: InstrumentationFlavorType,
        instrumentedJavaToolOptions: String?,
        otelResourceAttributes: Map<String, String>
    ) {

        //casting must succeed or we have a bug
        val myConfiguration = configuration as ExternalSystemRunConfiguration

        //keep the original env, we need to restore it after program start
        ExternalSystemConfigurationTempStorage.orgConfigurationEnvironmentVars[configuration] = configuration.settings.env



        if (!instrumentedJavaToolOptions.isNullOrBlank()) {

            var javaToolOptions = instrumentedJavaToolOptions

            //need to replace the env because it may be immutable map
            val newEnv = myConfiguration.settings.env.toMutableMap()

            val currentJavaToolOptions = newEnv[JAVA_TOOL_OPTIONS]

            if (currentJavaToolOptions != null) {

                //it's probably our JAVA_TOOL_OPTIONS that was not cleaned for some reason
                if (currentJavaToolOptions.trim().endsWith(DIGMA_MARKER)) {
                    newEnv.remove(JAVA_TOOL_OPTIONS)
                    //if we decided to remove JAVA_TOOL_OPTIONS then also remove it from the saved env that we
                    // keep for later cleaning
                    val orgEnv = ExternalSystemConfigurationTempStorage
                        .orgConfigurationEnvironmentVars[configuration]?.toMutableMap()
                    orgEnv?.remove(JAVA_TOOL_OPTIONS)
                } else {
                    javaToolOptions = smartMergeJavaToolOptions(instrumentedJavaToolOptions, currentJavaToolOptions)
                }
            }

            //mark the JavaToolOptions with DIGMA_MARKER, so that we know this it's ours, it helps protect against
            //a situation that we didn't clean the JAVA_TOOL_OPTIONS when the process started. we know it may happen sometimes
            // with GradleRunConfiguration.
            newEnv[JAVA_TOOL_OPTIONS] = javaToolOptions.plus(" $DIGMA_MARKER")
            myConfiguration.settings.env = newEnv
        }

        updateResourceAttribute(myConfiguration, params, instrumentationFlavorType, otelResourceAttributes)
    }


    private fun updateResourceAttribute(
        configuration: ExternalSystemRunConfiguration,
        params: SimpleProgramParameters,
        instrumentationFlavorType: InstrumentationFlavorType,
        ourOtelResourceAttributes: Map<String, String>
    ) {

        if (ourOtelResourceAttributes.isEmpty()) {
            return
        }


        when (instrumentationFlavorType) {
            InstrumentationFlavorType.Default,
            InstrumentationFlavorType.JavaServer,
            InstrumentationFlavorType.Micronaut,
            InstrumentationFlavorType.OpenLiberty,
            InstrumentationFlavorType.Quarkus -> updateOtelResourceAttribute(configuration, params, ourOtelResourceAttributes)

            InstrumentationFlavorType.SpringBootMicrometer -> updateSpringBootMicrometerResourceAttribute(
                configuration,
                params,
                ourOtelResourceAttributes
            )
        }
    }


    private fun updateOtelResourceAttribute(
        configuration: ExternalSystemRunConfiguration,
        params: SimpleProgramParameters,
        ourOtelResourceAttributes: Map<String, String>
    ) {

        val ourOtelResourceAttributesStr = mapToFlatString(ourOtelResourceAttributes)

        val newEnv = configuration.settings.env.toMutableMap()
        val currentOtelResourceAttributes = configuration.settings.env[OTEL_RESOURCE_ATTRIBUTES]
        val otelResourceAttributes = if (!currentOtelResourceAttributes.isNullOrBlank()) {
            currentOtelResourceAttributes.plus(",")
        } else {
            ""
        }.plus(ourOtelResourceAttributesStr)

        newEnv[OTEL_RESOURCE_ATTRIBUTES] = otelResourceAttributes
        configuration.settings.env = newEnv
    }


    private fun updateSpringBootMicrometerResourceAttribute(
        configuration: ExternalSystemRunConfiguration,
        params: SimpleProgramParameters,
        ourOtelResourceAttributes: Map<String, String>
    ) {
        //need to replace the map because its immutable
        val newEnv = configuration.settings.env.toMutableMap()
        newEnv.putAll(ourOtelResourceAttributes)
        configuration.settings.env = newEnv
    }

}