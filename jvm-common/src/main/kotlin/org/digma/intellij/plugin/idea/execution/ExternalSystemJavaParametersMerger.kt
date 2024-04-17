package org.digma.intellij.plugin.idea.execution

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleProgramParameters
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration

class ExternalSystemJavaParametersMerger(
    configuration: RunConfiguration,
    params: SimpleProgramParameters,
    parametersExtractor: ParametersExtractor
) : JavaParametersMerger(configuration, params, parametersExtractor) {

    override fun mergeJavaToolOptionsAndOtelResourceAttributes(instrumentedJavaToolOptions: String?, otelResourceAttributes: String?) {

        //casting must succeed or we have a bug
        val myConfiguration = configuration as ExternalSystemRunConfiguration

        if (instrumentedJavaToolOptions != null) {

            var javaToolOptions = instrumentedJavaToolOptions

            //need to replace the env because it may be immutable map
            val newEnv = myConfiguration.settings.env.toMutableMap()

            val currentJavaToolOptions = newEnv[JAVA_TOOL_OPTIONS]

            if (currentJavaToolOptions != null) {

                //it's probably our JAVA_TOOL_OPTIONS that was not cleaned for some reason
                if (currentJavaToolOptions.trim().endsWith(DIGMA_MARKER)) {
                    newEnv.remove(JAVA_TOOL_OPTIONS)
                } else {
                    javaToolOptions = smartMergeJavaToolOptions(javaToolOptions, currentJavaToolOptions)
                    newEnv[ORG_JAVA_TOOL_OPTIONS] = currentJavaToolOptions
                }
            }

            //mark the JavaToolOptions with DIGMA_MARKER, so that we know this it's ours, it helps protect against
            //a situation that we didn't clean the JAVA_TOOL_OPTIONS when the process started. we know it may happen sometimes
            // with GradleRunConfiguration.
            newEnv[JAVA_TOOL_OPTIONS] = javaToolOptions.plus(" $DIGMA_MARKER")
            myConfiguration.settings.env = newEnv
        }

        updateOtelResourceAttribute(myConfiguration, otelResourceAttributes)
    }


    private fun updateOtelResourceAttribute(configuration: ExternalSystemRunConfiguration, ourOtelResourceAttributes: String?) {

        if (ourOtelResourceAttributes != null) {

            val newEnv = configuration.settings.env.toMutableMap()
            val otelResourceAttributes = if (configuration.settings.env.containsKey(OTEL_RESOURCE_ATTRIBUTES)) {
                val currentOtelResourceAttributes = configuration.settings.env[OTEL_RESOURCE_ATTRIBUTES]
                newEnv[ORG_OTEL_RESOURCE_ATTRIBUTES] = currentOtelResourceAttributes
                currentOtelResourceAttributes.plus(",")
            } else {
                ""
            }.plus(ourOtelResourceAttributes)

            newEnv[OTEL_RESOURCE_ATTRIBUTES] = otelResourceAttributes
            configuration.settings.env = newEnv
        }
    }

}