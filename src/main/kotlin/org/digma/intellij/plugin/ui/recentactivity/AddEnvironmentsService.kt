package org.digma.intellij.plugin.ui.recentactivity

import com.intellij.execution.CommonProgramRunConfigurationParameters
import com.intellij.execution.RunManager
import com.intellij.execution.configuration.AbstractRunConfiguration
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.project.Project
import org.apache.maven.artifact.versioning.ComparableVersion
import org.digma.intellij.plugin.analytics.BackendInfoHolder
import org.digma.intellij.plugin.analytics.getEnvironmentById
import org.digma.intellij.plugin.analytics.isCentralized
import org.digma.intellij.plugin.auth.account.DigmaDefaultAccountHolder
import org.digma.intellij.plugin.common.newerThan
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.execution.DIGMA_ENVIRONMENT_ID_RESOURCE_ATTRIBUTE
import org.digma.intellij.plugin.idea.execution.DIGMA_ENVIRONMENT_NAME_RESOURCE_ATTRIBUTE
import org.digma.intellij.plugin.idea.execution.DIGMA_ENVIRONMENT_TYPE_RESOURCE_ATTRIBUTE
import org.digma.intellij.plugin.idea.execution.DIGMA_USER_ID_RESOURCE_ATTRIBUTE
import org.digma.intellij.plugin.idea.execution.OTEL_RESOURCE_ATTRIBUTES
import org.digma.intellij.plugin.idea.execution.flavor.SpringBootMicrometerInstrumentationFlavor
import org.digma.intellij.plugin.idea.execution.stringToMap
import org.digma.intellij.plugin.idea.frameworks.SpringBootMicrometerConfigureDepsService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.environment.EnvType

//app level service so includes pending environments from all projects
@Service(Service.Level.APP)
class AddEnvironmentsService {

    private val logger = Logger.getInstance(this::class.java)


    fun addToCurrentRunConfig(project: Project, environmentId: String): Boolean {
        val result = try {
            Log.log(logger::info, "addToCurrentRunConfig invoked for environment {}", environmentId)
            addDigmaEnvironmentToSelectedRunConfiguration(project, environmentId)
        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "failed adding environment {} to current run config", environmentId)
            ErrorReporter.getInstance().reportError(project, "AddEnvironmentsService.addToCurrentRunConfig", e)
            false
        }

        return result
    }


    //will only work for Idea IC/IU
    private fun addDigmaEnvironmentToSelectedRunConfiguration(project: Project, environmentId: String): Boolean {

        val selectedConfiguration = RunManager.getInstance(project).selectedConfiguration
        if (selectedConfiguration == null) {
            Log.log(logger::info, "could not find selected run config, not adding environment to current run config")
            return false
        }

        val config = selectedConfiguration.configuration

        val env = getEnvironmentById(project, environmentId)
        if (env == null) {
            Log.log(logger::info, "env not found by id {}, not adding environment to current run config", environmentId)
            return false
        }

        val userId = DigmaDefaultAccountHolder.getInstance().account?.userId
        if (userId == null) {
            Log.log(logger::info, "can not find user id, maybe user not logged in?, not adding environment to current run config")
            return false
        }

        val backendVersion = BackendInfoHolder.getInstance().getAbout()?.applicationVersion
        if (backendVersion == null) {
            Log.log(logger::info, "can not load backend version, not adding environment to current run config")
            return false
        }

        val backendVersionIs0315OrHigher = backendVersion.let {
            val backendVersionComparableVersion = ComparableVersion(it)
            val featureComparableVersion = ComparableVersion("0.3.15")
            backendVersionComparableVersion.newerThan(featureComparableVersion) ||
                    backendVersionComparableVersion == featureComparableVersion
        }

        val isCentralized = isCentralized(project)



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

        fun addEnvironmentWithFeatureFlagSpringBootWithMicroMeter(envVars: MutableMap<String, String>) {

            if (backendVersionIs0315OrHigher) {
                if(envVars.containsKey(SpringBootMicrometerInstrumentationFlavor.getEnvironmentIdAttributeKey())){
                    envVars.remove(SpringBootMicrometerInstrumentationFlavor.getEnvironmentIdAttributeKey())
                }
                if (isCentralized) {
                    envVars[SpringBootMicrometerInstrumentationFlavor.getEnvironmentNameAttributeKey()] = env.name
                    envVars[SpringBootMicrometerInstrumentationFlavor.getEnvironmentTypeAttributeKey()] = env.type.name
                    if(env.type ==  EnvType.Private) {//for public environment no need the userId
                        envVars[SpringBootMicrometerInstrumentationFlavor.getUserIdAttributeKey()] = userId
                    }

                } else {
                    envVars[SpringBootMicrometerInstrumentationFlavor.getEnvironmentNameAttributeKey()] = env.name
                }
            } else {
                envVars[SpringBootMicrometerInstrumentationFlavor.getEnvironmentIdAttributeKey()] = env.id
            }
        }


        fun addEnvironmentWithFeatureFlag(envVars: MutableMap<String, String>, valuesMap: MutableMap<String, String>) {

            if (backendVersionIs0315OrHigher) {
                if(valuesMap.containsKey(DIGMA_ENVIRONMENT_ID_RESOURCE_ATTRIBUTE)){
                    valuesMap.remove(DIGMA_ENVIRONMENT_ID_RESOURCE_ATTRIBUTE)
                }
                if (isCentralized) {
                    valuesMap[DIGMA_ENVIRONMENT_NAME_RESOURCE_ATTRIBUTE] = env.name
                    valuesMap[DIGMA_ENVIRONMENT_TYPE_RESOURCE_ATTRIBUTE] = env.type.name
                    if(env.type ==  EnvType.Private) {//for public environment no need the userId
                        valuesMap[DIGMA_USER_ID_RESOURCE_ATTRIBUTE] = userId
                    }
                } else {
                    valuesMap[DIGMA_ENVIRONMENT_NAME_RESOURCE_ATTRIBUTE] = env.name
                }

            } else {
                valuesMap[DIGMA_ENVIRONMENT_ID_RESOURCE_ATTRIBUTE] = environmentId
            }


            envVars[OTEL_RESOURCE_ATTRIBUTES] = valuesMap.entries
                .filter { entry -> entry.key.isNotBlank() }.joinToString(separator = ",")
        }


        //nested local function
        fun addEnvironmentToOtelResourceAttributes(envVars: MutableMap<String, String>) {
            if (isSpringBootWithMicroMeter(project, config)) {
                addEnvironmentWithFeatureFlagSpringBootWithMicroMeter(envVars)
            } else {

                //maybe OTEL_RESOURCE_ATTRIBUTES  already exists and has values other than digma.environment,
                // so preserve them

                val existingValue = envVars[OTEL_RESOURCE_ATTRIBUTES]

                if (existingValue != null) {

                    val valuesMap = stringToMap(existingValue)

                    addEnvironmentWithFeatureFlag(envVars, valuesMap)

                } else {
                    addEnvironmentWithFeatureFlag(envVars, mutableMapOf())
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