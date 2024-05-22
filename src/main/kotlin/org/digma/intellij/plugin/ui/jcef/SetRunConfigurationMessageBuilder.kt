package org.digma.intellij.plugin.ui.jcef

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.common.getEnvironmentMapFromRunConfiguration
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.execution.DIGMA_ENVIRONMENT_ID_RESOURCE_ATTRIBUTE
import org.digma.intellij.plugin.idea.execution.DIGMA_ENVIRONMENT_NAME_RESOURCE_ATTRIBUTE
import org.digma.intellij.plugin.idea.execution.DIGMA_ENVIRONMENT_TYPE_RESOURCE_ATTRIBUTE
import org.digma.intellij.plugin.idea.execution.DIGMA_USER_ID_RESOURCE_ATTRIBUTE
import org.digma.intellij.plugin.idea.execution.JavaToolOptionsDemoBuilder
import org.digma.intellij.plugin.idea.execution.OTEL_RESOURCE_ATTRIBUTES
import org.digma.intellij.plugin.idea.execution.flavor.SpringBootMicrometerInstrumentationFlavor
import org.digma.intellij.plugin.idea.execution.stringToMap
import org.digma.intellij.plugin.idea.frameworks.SpringBootMicrometerConfigureDepsService
import org.digma.intellij.plugin.ui.jcef.model.RunConfigObservabilityMode
import org.digma.intellij.plugin.ui.jcef.model.RunConfigurationAttributesPayload

class SetRunConfigurationMessageBuilder(
    private val project: Project,
    private val cefBrowser: CefBrowser,
    private val selectedConfiguration: RunnerAndConfigurationSettings?,
) {

    //hides classes that are available only in Idea and not in Rider.
    //it will be null in Rider.
    //we need it because in order to build a java tool options string we need to use classes that only
    // exists in Idea and not in Rider, in Rider we don't need to build java tool options.
    private val javaToolOptionsDemoBuilder: JavaToolOptionsDemoBuilder? = serviceOrNull<JavaToolOptionsDemoBuilder>()

    fun sendRunConfigurationAttributes() {

        try {
            val configuration = selectedConfiguration?.configuration

            if (configuration == null) {
                sendEmpty()
                return
            }


            if (!isSupported(configuration)) {
                sendNotSupportedWithJavaToolOptions(configuration)
                return
            }


            val payload = buildPayloadWithAttributes(configuration)
            payload?.let {
                sendRunConfigurationAttributes(project, cefBrowser, it)
            } ?: sendEmpty()

        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("SetRunConfigurationMessageBuilder.sendRunConfigurationAttributes", e)
            sendEmpty()
        }
    }


    private fun sendEmpty(isSupported: Boolean = true) {
        sendRunConfigurationAttributes(
            project,
            cefBrowser, RunConfigurationAttributesPayload(
                null,
                null,
                null,
                null,
                null,
                isSupported,
                null
            )
        )
    }


    private fun buildPayloadWithAttributes(configuration: RunConfiguration): RunConfigurationAttributesPayload? {

        val envVars = getEnvironmentMapFromRunConfiguration(configuration)
        if (envVars != null) {
            if (envVars.contains(OTEL_RESOURCE_ATTRIBUTES)) {
                val existingValue = envVars[OTEL_RESOURCE_ATTRIBUTES]
                if (existingValue != null) {
                    val attributes = stringToMap(existingValue)
                    val environmentId: String? = attributes[DIGMA_ENVIRONMENT_ID_RESOURCE_ATTRIBUTE]
                    val environmentName: String? = attributes[DIGMA_ENVIRONMENT_NAME_RESOURCE_ATTRIBUTE]
                    val environmentType: String? = attributes[DIGMA_ENVIRONMENT_TYPE_RESOURCE_ATTRIBUTE]
                    val userId: String? = attributes[DIGMA_USER_ID_RESOURCE_ATTRIBUTE]
                    return RunConfigurationAttributesPayload(
                        environmentId,
                        environmentName,
                        environmentType,
                        userId,
                        RunConfigObservabilityMode.OtelAgent,
                        true,
                        null
                    )
                }
            } else if (SpringBootMicrometerConfigureDepsService.isSpringBootWithMicrometer()) {

                val environmentId: String? = envVars[SpringBootMicrometerInstrumentationFlavor.getEnvironmentIdAttributeKey()]
                val environmentName: String? = envVars[SpringBootMicrometerInstrumentationFlavor.getEnvironmentNameAttributeKey()]
                val environmentType: String? = envVars[SpringBootMicrometerInstrumentationFlavor.getEnvironmentTypeAttributeKey()]
                val userId: String? = envVars[SpringBootMicrometerInstrumentationFlavor.getUserIdAttributeKey()]
                if (environmentId != null || environmentName != null) {
                    return RunConfigurationAttributesPayload(
                        environmentId,
                        environmentName,
                        environmentType,
                        userId,
                        RunConfigObservabilityMode.Micrometer,
                        true,
                        null
                    )
                }
            }
        }

        return null

    }


    private fun isSupported(configuration: RunConfiguration): Boolean {
        return javaToolOptionsDemoBuilder?.isSupported(configuration) ?: false
    }


    private fun sendNotSupportedWithJavaToolOptions(configuration: RunConfiguration) {

        javaToolOptionsDemoBuilder?.let {
            val javaToolOptions = it.buildDemoJavaToolOptions(configuration)
            val observabilityMode = if (SpringBootMicrometerConfigureDepsService.isSpringBootWithMicrometer()) {
                RunConfigObservabilityMode.Micrometer
            } else {
                RunConfigObservabilityMode.OtelAgent
            }

            val payload = RunConfigurationAttributesPayload(
                null,
                null,
                null,
                null,
                observabilityMode,
                false,
                javaToolOptions
            )

            sendRunConfigurationAttributes(project, cefBrowser, payload)
        } ?: sendEmpty(false)
    }

}