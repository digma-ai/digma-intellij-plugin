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
        pendingEnvironments.add(environment)
        flush()
    }

    fun removeEnvironment(environment: String) {
        pendingEnvironments.remove(environment)
        flush()
    }

    private fun flush() {
        try {
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
                jsonObject.forEach {
                    pendingEnvironments.add(it.asText())
                }
            }

        } catch (e: Exception) {
            Log.warnWithException(logger, project, e, "Error loading pending environments")
            service<ErrorReporter>().reportError("AddEnvironmentsService.load", e)
        }

    }

    fun addToCurrentRunConfig(environemnt: String): Boolean {
        Log.log(logger::info, project, "adding environment {} to current run config", environemnt)
        val selectedConfiguration = RunManager.getInstance(project).selectedConfiguration
        selectedConfiguration?.let {
            val config = it.configuration
            Log.log(logger::info, project, "found selected configuration {} type {}", config.name, config.type)
            if (config is CommonProgramRunConfigurationParameters) {
                Log.log(logger::info, project, "adding environment to configuration {}", config.name)
                config.envs.put("OTEL_RESOURCE_ATTRIBUTES", "digma.environment=$environemnt")
                return true
            } else if (config is ExternalSystemRunConfiguration) {
                config.settings.env.put("OTEL_RESOURCE_ATTRIBUTES", "digma.environment=$environemnt")
                return true
            } else if (config is AbstractRunConfiguration) {
                config.envs.put("OTEL_RESOURCE_ATTRIBUTES", "digma.environment=$environemnt")
                return true
            } else {
                Log.log(logger::info, project, "configuration {} is not supported, not adding environment", config.name)
                return false
            }
        } ?: return false
    }
}