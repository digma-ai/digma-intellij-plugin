package org.digma.intellij.plugin.ui.jcef

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.application.ApplicationConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleProgramParameters
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.common.getEnvironmentMapFromRunConfiguration
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.execution.DIGMA_ENVIRONMENT_ID_RESOURCE_ATTRIBUTE
import org.digma.intellij.plugin.idea.execution.DIGMA_ENVIRONMENT_NAME_RESOURCE_ATTRIBUTE
import org.digma.intellij.plugin.idea.execution.DIGMA_ENVIRONMENT_TYPE_RESOURCE_ATTRIBUTE
import org.digma.intellij.plugin.idea.execution.DIGMA_USER_ID_RESOURCE_ATTRIBUTE
import org.digma.intellij.plugin.idea.execution.OTEL_RESOURCE_ATTRIBUTES
import org.digma.intellij.plugin.idea.execution.RunConfigurationHandlersHolder
import org.digma.intellij.plugin.idea.execution.flavor.SpringBootMicrometerInstrumentationFlavor
import org.digma.intellij.plugin.idea.execution.stringToMap
import org.digma.intellij.plugin.idea.frameworks.SpringBootMicrometerConfigureDepsService
import org.digma.intellij.plugin.ui.jcef.model.RunConfigObservabilityMode
import org.digma.intellij.plugin.ui.jcef.model.RunConfigurationAttributesPayload

class SetRunConfigurationMessageBuilder(
    private val cefBrowser: CefBrowser,
    private val selectedConfiguration: RunnerAndConfigurationSettings?
) {


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
                sendRunConfigurationAttributes(cefBrowser, it)
            } ?: sendEmpty()

        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("SetRunConfigurationMessageBuilder.sendRunConfigurationAttributes", e)
            sendEmpty()
        }
    }


    private fun sendEmpty(isSupported: Boolean = true) {
        sendRunConfigurationAttributes(
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
        return RunConfigurationHandlersHolder.runConfigurationHandlers.find { it.isApplicableFor(configuration) } != null
    }


    private fun sendNotSupportedWithJavaToolOptions(configuration: RunConfiguration) {

        //we want to build java tool options. instead of refactoring all related classes we can use a dummy
        // configuration and run the RunConfigurationInstrumentationHandler on it. fortunately the method
        // updateParameters returns the java tool options and we can use it.
        // using ApplicationConfiguration makes sure its supported.

        try {
            val factory = ApplicationConfigurationType.getInstance().configurationFactories.first()
            val template = factory.createTemplateConfiguration(configuration.project)
            val dummyConfig = factory.createConfiguration("dummy", template)

            val handler = RunConfigurationHandlersHolder.runConfigurationHandlers.find { it.isApplicableFor(dummyConfig) }

            if (handler == null) {
                sendEmpty(false)
                return
            }


            val instrumentationParams = handler.updateParameters(dummyConfig, SimpleProgramParameters(), null)
            if (instrumentationParams != null) {
                val javaToolOptions = instrumentationParams.first

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

                sendRunConfigurationAttributes(cefBrowser, payload)

            } else {
                sendEmpty(false)
            }

        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("SetRunConfigurationMessageBuilder.sendNotSupportedWithJavaToolOptions", e)
            sendEmpty(false)
        }
    }


}