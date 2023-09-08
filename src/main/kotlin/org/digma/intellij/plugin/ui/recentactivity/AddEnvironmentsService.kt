package org.digma.intellij.plugin.ui.recentactivity

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.intellij.execution.CommonProgramRunConfigurationParameters
import com.intellij.execution.RunManager
import com.intellij.execution.configuration.AbstractRunConfiguration
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.ui.recentactivity.model.AdditionToConfigResult
import org.digma.intellij.plugin.ui.recentactivity.model.PendingEnvironment

//app level service so includes pending environments from all projects
@Service(Service.Level.APP)
class AddEnvironmentsService {

    private val logger = Logger.getInstance(this::class.java)

    private val pendingEnvironments = mutableMapOf<String, PendingEnvironment>()

    private val objectMapper: ObjectMapper = ObjectMapper()

    init {
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        objectMapper.setDateFormat(StdDateFormat())
        load()
    }


    fun getPendingEnvironments(): Map<String, PendingEnvironment> {
        return pendingEnvironments
    }

    fun addEnvironment(environment: String) {
        Log.log(logger::info, "adding environment {}", environment)
        pendingEnvironments[environment] = PendingEnvironment(environment)
        flush()
    }

    fun removeEnvironment(environment: String) {
        Log.log(logger::info, "removing environment {}", environment)
        pendingEnvironments.remove(environment)
        flush()
    }

    private fun flush() {
        try {
            Log.log(logger::info, "flushing environments {}", pendingEnvironments)
            val asJson = objectMapper.writeValueAsString(pendingEnvironments.values)
            service<PersistenceService>().state.pendingEnvironment = asJson
        } catch (e: Exception) {
            Log.warnWithException(logger, e, "Error flushing pending environments")
            service<ErrorReporter>().reportError("AddEnvironmentsService.flush", e)
        }
    }

    private fun load() {
        try {
            val asJson = service<PersistenceService>().state.pendingEnvironment
            asJson?.let {
                val jsonObject: ArrayNode = objectMapper.readTree(it) as ArrayNode
                jsonObject.forEach { jsonNode ->
                    val name = jsonNode.get("name").asText()
                    val additionToConfigResult: AdditionToConfigResult? = try {
                        AdditionToConfigResult.valueOf(jsonNode.get("additionToConfigResult").asText())
                    } catch (e: Exception) {
                        null
                    }

                    pendingEnvironments[name] = PendingEnvironment(name, additionToConfigResult)
                }
                Log.log(logger::info, "loaded environments {}", pendingEnvironments)
            }

        } catch (e: Exception) {
            Log.warnWithException(logger, e, "Error loading pending environments")
            service<ErrorReporter>().reportError("AddEnvironmentsService.load", e)
        }

    }

    fun addToCurrentRunConfig(project: Project, environment: String): Boolean {
        val result = try {
            Log.log(logger::info, "addToCurrentRunConfig invoked for environment {}", environment)
            addToCurrentRunConfigImpl(project, environment)
        } catch (e: Exception) {
            Log.warnWithException(logger, e, "failed adding environment {} to current run config", environment)
            service<ErrorReporter>().reportError("AddEnvironmentsService.addToCurrentRunConfig", e)
            false
        }

        //not sure necessary, just in case addToCurrentRunConfig is called for environment that is not
        // in the map, it may happen if flush failed
        val pendingEnv = if (pendingEnvironments[environment] == null) {
            addEnvironment(environment)
            pendingEnvironments[environment]
        } else {
            pendingEnvironments[environment]
        }

        pendingEnv?.let {
            it.additionToConfigResult = if (result) AdditionToConfigResult.success else AdditionToConfigResult.failure
        }
        flush()
        return result
    }


    private fun addToCurrentRunConfigImpl(project: Project, environment: String): Boolean {

        val selectedConfiguration = RunManager.getInstance(project).selectedConfiguration
        if (selectedConfiguration == null) {
            Log.log(logger::info, "could not find selected run config, not adding environment")
            return false
        }

        val config = selectedConfiguration.configuration

        Log.log(logger::info, "found selected configuration {} type {}", config.name, config.type)

        return when (config) {
            is CommonProgramRunConfigurationParameters -> {
                Log.log(logger::info, "adding environment to configuration {}", config.name)
                try {
                    config.envs["OTEL_RESOURCE_ATTRIBUTES"] = "digma.environment=$environment"
                } catch (e: Exception) {
                    Log.log(logger::info, "failed adding environment to configuration {},{}, trying to replace map", config.name, e)
                    val map = mutableMapOf<String, String>()
                    map.putAll(config.envs)
                    addEnvToMap(map, environment)
                    config.envs = map
                }
                true
            }

            is ExternalSystemRunConfiguration -> {
                Log.log(logger::info, "adding environment to configuration {}", config.name)
                try {
                    config.settings.env["OTEL_RESOURCE_ATTRIBUTES"] = "digma.environment=$environment"
                } catch (e: Exception) {
                    Log.log(logger::info, "failed adding environment to configuration {},{}, trying to replace map", config.name, e)
                    val map = mutableMapOf<String, String>()
                    map.putAll(config.settings.env)
                    addEnvToMap(map, environment)
                    config.settings.env = map
                }
                true
            }

            is AbstractRunConfiguration -> {
                Log.log(logger::info, "adding environment to configuration {}", config.name)
                try {
                    config.envs["OTEL_RESOURCE_ATTRIBUTES"] = "digma.environment=$environment"
                } catch (e: Exception) {
                    Log.log(logger::info, "failed adding environment to configuration {},{}, trying to replace map", config.name, e)
                    val map = mutableMapOf<String, String>()
                    map.putAll(config.envs)
                    addEnvToMap(map, environment)
                    config.envs = map
                }
                true
            }

            else -> {
                Log.log(logger::info, "configuration {} is not supported, not adding environment", config.name)
                false
            }
        }
    }

    private fun addEnvToMap(map: MutableMap<String, String>, environment: String) {
        map["OTEL_RESOURCE_ATTRIBUTES"] = "digma.environment=$environment"
    }
}