package org.digma.intellij.plugin.ui.recentactivity

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.intellij.execution.CommonProgramRunConfigurationParameters
import com.intellij.execution.RunManager
import com.intellij.execution.configuration.AbstractRunConfiguration
import com.intellij.execution.configurations.ModuleBasedConfiguration
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.project.Project
import kotlinx.collections.immutable.toImmutableMap
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.deps.ModulesDepsService
import org.digma.intellij.plugin.idea.frameworks.SpringBootMicrometerConfigureDepsService
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

    private val objectMapper: ObjectMapper = ObjectMapper()

    init {
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        objectMapper.setDateFormat(StdDateFormat())
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
            service<PersistenceService>().state.pendingEnvironment = asJson
        } catch (e: Exception) {
            Log.warnWithException(logger, e, "Error flushing pending environments")
            service<ErrorReporter>().reportError("AddEnvironmentsService.flush", e)
        }
    }

    private fun load() {
        try {

            Log.log(logger::info, "loading environments from persistence")

            val asJson = service<PersistenceService>().state.pendingEnvironment
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
            addToCurrentRunConfigImpl(project, environment)
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

    private fun isSpringBootWithMicroMeter(project: Project, selectedRunConfig: RunConfiguration): Boolean {
        if (SpringBootMicrometerConfigureDepsService.isSpringBootWithMicrometer()) {
            if (selectedRunConfig is ModuleBasedConfiguration<*, *>) {
                var isSpringBootModule = false
                selectedRunConfig.configurationModule.module?.let { module ->
                    val modulesDepsService = ModulesDepsService.getInstance(project)
                    isSpringBootModule = modulesDepsService.isSpringBootModule(module)
                }
                return isSpringBootModule
            }
        }
        return false
    }

    private fun addToCurrentRunConfigImpl(project: Project, environment: String): Boolean {

        val selectedConfiguration = RunManager.getInstance(project).selectedConfiguration
        if (selectedConfiguration == null) {
            Log.log(logger::info, "could not find selected run config, not adding environment")
            return false
        }

        val config = selectedConfiguration.configuration

        Log.log(logger::info, "found selected configuration {} type {}", config.name, config.type)

        var envVarKey = "OTEL_RESOURCE_ATTRIBUTES"
        var envVarValue = "digma.environment=$environment"
        if (isSpringBootWithMicroMeter(project, config)) {
            // note: digma_environment contains underscore on purpose - spring will transform it to digma.environment (underscore becomes dot)
            envVarKey = "MANAGEMENT_OPENTELEMETRY_RESOURCE-ATTRIBUTES_digma_environment"
            envVarValue = environment
        }

        return when (config) {
            is CommonProgramRunConfigurationParameters -> {
                Log.log(logger::info, "adding environment to configuration {}", config.name)
                config.envs = config.envs.toMutableMap()
                config.envs[envVarKey] = envVarValue

                true
            }

            is ExternalSystemRunConfiguration -> {
                Log.log(logger::info, "adding environment to configuration {}", config.name)
                config.settings.env = config.settings.env.toMutableMap()
                config.settings.env[envVarKey] = envVarValue

                true
            }

            is AbstractRunConfiguration -> {
                Log.log(logger::info, "adding environment to configuration {}", config.name)
                config.envs = config.envs.toMutableMap()
                config.envs[envVarKey] = envVarValue

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