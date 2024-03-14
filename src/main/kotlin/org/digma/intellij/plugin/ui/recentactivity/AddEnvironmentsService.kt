package org.digma.intellij.plugin.ui.recentactivity

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.intellij.execution.CommonProgramRunConfigurationParameters
import com.intellij.execution.RunManager
import com.intellij.execution.configuration.AbstractRunConfiguration
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.common.createObjectMapper
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.frameworks.SpringBootMicrometerConfigureDepsService
import org.digma.intellij.plugin.idea.runcfg.DIGMA_ENVIRONMENT_RESOURCE_ATTRIBUTE
import org.digma.intellij.plugin.idea.runcfg.OTEL_RESOURCE_ATTRIBUTES
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.ui.recentactivity.model.AdditionToConfigResult
import org.digma.intellij.plugin.ui.recentactivity.model.EnvironmentType
import org.digma.intellij.plugin.ui.recentactivity.model.PendingEnvironment

//app level service so includes pending environments from all projects
@Service(Service.Level.APP)
class AddEnvironmentsService {

    private val logger = Logger.getInstance(this::class.java)

    private val pendingEnvironments = mutableListOf<PendingEnvironment>()

    private val objectMapper: ObjectMapper = createObjectMapper()

    init {
        load()
    }


    fun getPendingEnvironments(): List<PendingEnvironment> {
        return pendingEnvironments
    }

    fun addEnvironment(environment: String) {
        Log.log(logger::info, "adding environment {}", environment)
        pendingEnvironments.add(PendingEnvironment(environment))
        flush()
    }

    fun getPendingEnvironment(environment: String): PendingEnvironment? {
        return pendingEnvironments.find { pendingEnvironment -> pendingEnvironment.name == environment }
    }

    fun isPendingEnv(environment: String): Boolean {
        return pendingEnvironments.any { pendingEnvironment -> pendingEnvironment.name == environment }
    }

    fun removeEnvironment(environment: String) {
        Log.log(logger::info, "removing environment {}", environment)
        pendingEnvironments.removeIf { p: PendingEnvironment -> p.name == environment }
        flush()
    }

    fun setEnvironmentType(project: Project, environment: String, type: String?) {
        val pendingEnvironment: PendingEnvironment? = pendingEnvironments.find { pendingEnvironment -> pendingEnvironment.name == environment }
        pendingEnvironment?.let {

            if (type == null) {
                it.type = null
            } else {
                it.type = try {
                    EnvironmentType.valueOf(type)
                } catch (e: Exception) {
                    ErrorReporter.getInstance().reportError(project, "AddEnvironmentsService.setEnvironmentType", e)
                    null
                }
            }
        }
        flush()
    }


    fun setEnvironmentServerUrl(project: Project, environment: String, serverUrl: String, token: String?) {
        val pendingEnvironment: PendingEnvironment? = pendingEnvironments.find { pendingEnvironment -> pendingEnvironment.name == environment }
        pendingEnvironment?.let {
            try {
                it.serverApiUrl = serverUrl
                it.token = token
            } catch (e: Exception) {
                ErrorReporter.getInstance().reportError(project, "AddEnvironmentsService.setEnvironmentServerUrl", e)
            }
        }
        flush()
    }

    fun setEnvironmentSetupFinished(project: Project, environment: String) {
        val pendingEnvironment: PendingEnvironment? = pendingEnvironments.find { pendingEnvironment -> pendingEnvironment.name == environment }
        pendingEnvironment?.let {
            try {
                it.isOrgDigmaSetupFinished = true
            } catch (e: Exception) {
                ErrorReporter.getInstance().reportError(project, "AddEnvironmentsService.setEnvironmentSetupFinished", e)
            }
        }
        flush()
    }


    private fun flush() {
        try {
            Log.log(logger::info, "flushing environments {}", pendingEnvironments)
            val asJson = objectMapper.writeValueAsString(pendingEnvironments)
            service<PersistenceService>().setPendingEnvironment(asJson)
        } catch (e: Exception) {
            Log.warnWithException(logger, e, "Error flushing pending environments")
            service<ErrorReporter>().reportError("AddEnvironmentsService.flush", e)
        }
    }

    private fun load() {
        try {

            Log.log(logger::info, "loading environments from persistence")

            val asJson = service<PersistenceService>().getPendingEnvironment()
            asJson?.let {
                val jsonObject: ArrayNode = objectMapper.readTree(it) as ArrayNode
                jsonObject.forEach { jsonNode ->
                    val pendingEnv = objectMapper.readValue(jsonNode.toString(), PendingEnvironment::class.java)
                    pendingEnvironments.add(pendingEnv)
                }
            }

            Log.log(logger::info, "loaded environments {}", pendingEnvironments)

        } catch (e: Exception) {
            Log.warnWithException(logger, e, "Error loading pending environments")
            service<ErrorReporter>().reportError("AddEnvironmentsService.load", e)
        }
    }


    fun addToCurrentRunConfig(project: Project, environment: String): Boolean {
        val result = try {
            Log.log(logger::info, "addToCurrentRunConfig invoked for environment {}", environment)
            addDigmaEnvironmentToSelectedRunConfiguration(project, environment)
        } catch (e: Exception) {
            Log.warnWithException(logger, e, "failed adding environment {} to current run config", environment)
            service<ErrorReporter>().reportError(project, "AddEnvironmentsService.addToCurrentRunConfig", e)
            false
        }

        //not sure necessary, just in case addToCurrentRunConfig is called for environment that is not
        // in the map, it may happen if flush failed
        if (pendingEnvironments.none { pendingEnvironment -> pendingEnvironment.name == environment }) {
            addEnvironment(environment)
        }

        val pendingEnv: PendingEnvironment? = pendingEnvironments.find { pendingEnvironment -> pendingEnvironment.name == environment }

        pendingEnv?.let {
            it.additionToConfigResult = if (result) AdditionToConfigResult.success else AdditionToConfigResult.failure
        }
        flush()
        return result
    }


    //will only work for idea
    private fun addDigmaEnvironmentToSelectedRunConfiguration(project: Project, environment: String): Boolean {

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
                envVars["MANAGEMENT_OPENTELEMETRY_RESOURCE-ATTRIBUTES_digma_environment"] = environment
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
//                    existingValue.split(",").associate {
//                        val (left, right) = it.split("=")
//                        left to right
//                    }.toMutableMap()
                    } catch (e: Throwable) {
                        mutableMapOf()
                    }

                    valuesMap[DIGMA_ENVIRONMENT_RESOURCE_ATTRIBUTE] = environment

                    envVars[OTEL_RESOURCE_ATTRIBUTES] = valuesMap.entries
                        .filter { entry -> !entry.key.isNullOrBlank() }.joinToString(separator = ",")

                } else {
                    envVars[OTEL_RESOURCE_ATTRIBUTES] = "$DIGMA_ENVIRONMENT_RESOURCE_ATTRIBUTE=$environment"
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