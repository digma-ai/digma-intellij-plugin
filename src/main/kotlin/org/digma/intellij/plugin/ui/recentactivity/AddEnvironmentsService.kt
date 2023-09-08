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

@Service(Service.Level.PROJECT)
class AddEnvironmentsService(val project: Project) {

    private val logger = Logger.getInstance(this::class.java)

    private val pendingEnvironments = mutableListOf<String>()

    private val objectMapper: ObjectMapper = ObjectMapper()

    init {
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        objectMapper.setDateFormat(StdDateFormat())
        load()
    }


    fun getPendingEnvironments(): List<String> {
        return pendingEnvironments
    }

    fun addEnvironment(environment: String) {
        Log.log(logger::info, project, "adding environment {}", environment)
        pendingEnvironments.add(environment)
        flush()
    }

    fun removeEnvironment(environment: String) {
        Log.log(logger::info, project, "removing environment {}", environment)
        pendingEnvironments.remove(environment)
        flush()
    }

    private fun flush() {
        try {
            Log.log(logger::info, project, "flushing environments {}", pendingEnvironments)
            val asJson = objectMapper.writeValueAsString(pendingEnvironments)
            service<PersistenceService>().state.pendingEnvironment = asJson
        } catch (e: Exception) {
            Log.warnWithException(logger, project, e, "Error flushing pending environments")
            service<ErrorReporter>().reportError("AddEnvironmentsService.flush", e)
        }
    }

    private fun load() {
        try {
            val asJson = service<PersistenceService>().state.pendingEnvironment
            asJson?.let {
                val jsonObject: ArrayNode = objectMapper.readTree(it) as ArrayNode
                jsonObject.forEach { jsonNode ->
                    pendingEnvironments.add(jsonNode.asText())
                }
                Log.log(logger::info, project, "loaded environments {}", pendingEnvironments)
            }

        } catch (e: Exception) {
            Log.warnWithException(logger, project, e, "Error loading pending environments")
            service<ErrorReporter>().reportError("AddEnvironmentsService.load", e)
        }

    }

    fun addToCurrentRunConfig(environment: String): Boolean {
        return try {
            Log.log(logger::info, project, "addToCurrentRunConfig invoked for environment {}", environment)
            addToCurrentRunConfigImpl(environment)
        } catch (e: Exception) {
            Log.warnWithException(logger, project, e, "failed adding environment {} to current run config", environment)
            service<ErrorReporter>().reportError("AddEnvironmentsService.addToCurrentRunConfig", e)
            false
        }
    }


    private fun addToCurrentRunConfigImpl(environment: String): Boolean {

        val selectedConfiguration = RunManager.getInstance(project).selectedConfiguration
        if (selectedConfiguration == null) {
            Log.log(logger::info, project, "could not find selected run config, not adding environment")
            return false
        }

        val config = selectedConfiguration.configuration

        Log.log(logger::info, project, "found selected configuration {} type {}", config.name, config.type)

        return when (config) {
            is CommonProgramRunConfigurationParameters -> {
                Log.log(logger::info, project, "adding environment to configuration {}", config.name)
                try {
                    config.envs["OTEL_RESOURCE_ATTRIBUTES"] = "digma.environment=$environment"
                } catch (e: Exception) {
                    Log.log(logger::info, project, "failed adding environment to configuration {},{}, trying to replace map", config.name, e)
                    val map = mutableMapOf<String, String>()
                    map.putAll(config.envs)
                    addEnvToMap(map, environment)
                    config.envs = map
                }
                true
            }

            is ExternalSystemRunConfiguration -> {
                Log.log(logger::info, project, "adding environment to configuration {}", config.name)
                try {
                    config.settings.env["OTEL_RESOURCE_ATTRIBUTES"] = "digma.environment=$environment"
                } catch (e: Exception) {
                    Log.log(logger::info, project, "failed adding environment to configuration {},{}, trying to replace map", config.name, e)
                    val map = mutableMapOf<String, String>()
                    map.putAll(config.settings.env)
                    addEnvToMap(map, environment)
                    config.settings.env = map
                }
                true
            }

            is AbstractRunConfiguration -> {
                Log.log(logger::info, project, "adding environment to configuration {}", config.name)
                try {
                    config.envs["OTEL_RESOURCE_ATTRIBUTES"] = "digma.environment=$environment"
                } catch (e: Exception) {
                    Log.log(logger::info, project, "failed adding environment to configuration {},{}, trying to replace map", config.name, e)
                    val map = mutableMapOf<String, String>()
                    map.putAll(config.envs)
                    addEnvToMap(map, environment)
                    config.envs = map
                }
                true
            }

            else -> {
                Log.log(logger::info, project, "configuration {} is not supported, not adding environment", config.name)
                false
            }
        }
    }

    private fun addEnvToMap(map: MutableMap<String, String>, environment: String) {
        map["OTEL_RESOURCE_ATTRIBUTES"] = "digma.environment=$environment"
    }
}