package org.digma.intellij.plugin.idea.execution

import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.SimpleProgramParameters
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import org.digma.intellij.plugin.vcs.VcsService

class ExternalSystemJavaToolOptionsMerger(
    configuration: RunConfigurationBase<*>,
    params: SimpleProgramParameters,
    parametersExtractor: ParametersExtractor
) : JavaToolOptionsMerger(configuration, params, parametersExtractor) {

    override fun mergeJavaToolOptions(instrumentedJavaToolOptions: String) {

        //casting must succeed or we have a bug
        val myConfiguration = configuration as ExternalSystemRunConfiguration

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

        updateOtelResourceAttribute(myConfiguration)
    }


    private fun updateOtelResourceAttribute(configuration: ExternalSystemRunConfiguration) {

        val commitId = VcsService.getInstance(configuration.project).getCommitIdForCurrentProject()
            ?: return

        val newEnv = configuration.settings.env.toMutableMap()
        val otelResourceAttributes = if (configuration.settings.env.containsKey(OTEL_RESOURCE_ATTRIBUTES)) {
            val currentOtelResourceAttributes = configuration.settings.env[OTEL_RESOURCE_ATTRIBUTES]
            newEnv[ORG_OTEL_RESOURCE_ATTRIBUTES] = currentOtelResourceAttributes
            currentOtelResourceAttributes.plus(",")
        } else {
            ""
        }.plus("scm.commit.id=$commitId")

        newEnv[OTEL_RESOURCE_ATTRIBUTES] = otelResourceAttributes
        configuration.settings.env = newEnv
    }

}