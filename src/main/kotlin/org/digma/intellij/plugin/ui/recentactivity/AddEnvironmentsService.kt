package org.digma.intellij.plugin.ui.recentactivity

import com.intellij.execution.CommonProgramRunConfigurationParameters
import com.intellij.execution.RunManager
import com.intellij.execution.configuration.AbstractRunConfiguration
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.execution.DIGMA_ENVIRONMENT_ID_RESOURCE_ATTRIBUTE
import org.digma.intellij.plugin.idea.execution.OTEL_RESOURCE_ATTRIBUTES
import org.digma.intellij.plugin.idea.execution.flavor.SpringBootMicrometerInstrumentationFlavor
import org.digma.intellij.plugin.idea.frameworks.SpringBootMicrometerConfigureDepsService
import org.digma.intellij.plugin.log.Log

//app level service so includes pending environments from all projects
@Service(Service.Level.APP)
class AddEnvironmentsService {

    private val logger = Logger.getInstance(this::class.java)


    fun addToCurrentRunConfig(project: Project, environmentId: String): Boolean {
        val result = try {
            Log.log(logger::info, "addToCurrentRunConfig invoked for environment {}", environmentId)
            addDigmaEnvironmentToSelectedRunConfiguration(project, environmentId)
        } catch (e: Exception) {
            Log.warnWithException(logger, e, "failed adding environment {} to current run config", environmentId)
            service<ErrorReporter>().reportError(project, "AddEnvironmentsService.addToCurrentRunConfig", e)
            false
        }

        return result
    }


    //will only work for idea
    private fun addDigmaEnvironmentToSelectedRunConfiguration(project: Project, environmentId: String): Boolean {

        val selectedConfiguration = RunManager.getInstance(project).selectedConfiguration
        if (selectedConfiguration == null) {
            Log.log(logger::info, "could not find selected run config, not adding environment")
            return false
        }

        val config = selectedConfiguration.configuration

        Log.log(logger::info, "found selected configuration {} type {}", config.name, config.type)

        //nested local function
        fun isSpringBootWithMicroMeter(project: Project, selectedRunConfig: RunConfiguration): Boolean {
            //todo: check if its really a spring boot module
//                if (selectedRunConfig is ModuleBasedConfiguration<*, *>) {
//                    var isSpringBootModule = false
//                    selectedRunConfig.configurationModule.module?.let { module ->
//                        val modulesDepsService = ModulesDepsService.getInstance(project)
//                        isSpringBootModule = modulesDepsService.isSpringBootModule(module)
//                    }
//                    return isSpringBootModule
//                }
            return SpringBootMicrometerConfigureDepsService.isSpringBootWithMicrometer()
        }

        //nested local function
        fun addEnvironmentToOtelResourceAttributes(envVars: MutableMap<String, String>) {
            if (isSpringBootWithMicroMeter(project, config)) {
                envVars[SpringBootMicrometerInstrumentationFlavor.getEnvironmentIdAttributeKey()] = environmentId
            } else {
                //maybe OTEL_RESOURCE_ATTRIBUTES  already exists and has values other than digma.environment,
                // so preserve them

                val existingValue = envVars[OTEL_RESOURCE_ATTRIBUTES]

                if (existingValue != null) {
                    val valuesMap = try {
                        existingValue.split(",").associate {
                            val entry = it.split("=")
                            var left: String? = null
                            var right: String? = null
                            if (entry.size == 1) {
                                left = entry[0]
                                right = ""
                            } else if (entry.size == 2) {
                                left = entry[0]
                                right = entry[1]
                            }
                            left to right
                        }.toMutableMap()
                    } catch (e: Throwable) {
                        mutableMapOf()
                    }

                    valuesMap[DIGMA_ENVIRONMENT_ID_RESOURCE_ATTRIBUTE] = environmentId

                    envVars[OTEL_RESOURCE_ATTRIBUTES] = valuesMap.entries
                        .filter { entry -> !entry.key.isNullOrBlank() }.joinToString(separator = ",")

                } else {
                    envVars[OTEL_RESOURCE_ATTRIBUTES] = "$DIGMA_ENVIRONMENT_ID_RESOURCE_ATTRIBUTE=$environmentId"
                }
            }
        }

        return when (config) {
            is CommonProgramRunConfigurationParameters -> {
                Log.log(logger::info, "adding environment to configuration {}", config.name)
                config.envs = config.envs.toMutableMap()
                addEnvironmentToOtelResourceAttributes(config.envs)
                true
            }
            is ExternalSystemRunConfiguration -> {
                Log.log(logger::info, "adding environment to configuration {}", config.name)
                config.settings.env = config.settings.env.toMutableMap()
                addEnvironmentToOtelResourceAttributes(config.settings.env)
                true
            }
            is AbstractRunConfiguration -> {
                Log.log(logger::info, "adding environment to configuration {}", config.name)
                config.envs = config.envs.toMutableMap()
                addEnvironmentToOtelResourceAttributes(config.envs)
                true
            }
            else -> {
                Log.log(logger::info, "configuration {} is not supported, not adding environment", config.name)
                false
            }
        }
    }


}