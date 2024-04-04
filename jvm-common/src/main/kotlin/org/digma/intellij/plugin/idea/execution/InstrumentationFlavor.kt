package org.digma.intellij.plugin.idea.execution

import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.openapi.module.Module


const val INSTRUMENTATION_FLAVOR_ENV_NAME = "INSTRUMENTATION_FLAVOR"

@Suppress("unused", "CanBeParameter", "MemberVisibilityCanBePrivate")
open class InstrumentationFlavor(
    private val configuration: RunConfigurationBase<*>,
    private val projectHeuristics: ProjectHeuristics,
    private val moduleResolver: ModuleResolver,
    private val parametersExtractor: ParametersExtractor
) {

    //todo: this class can have a getFlavor method to use all over. can init the flavor when the object is initialized.
    // but needs first to research a bit and decide on the possible flavors.
    // it can be changed without changing the API.
    // currently the API of isXXX is fine and maybe even clearer then getFlavor


    private enum class Flavors { Default, Micronaut, Quarkus, SpringBootMicrometer }

    private val resolvedModule: Module? = moduleResolver.resolveModule()

    private val flavorInEnv: Flavors? = try {
        val flavorValueInEnv = parametersExtractor.extractEnvValue(INSTRUMENTATION_FLAVOR_ENV_NAME)
        flavorValueInEnv?.let {
            Flavors.valueOf(it)
        }
    } catch (e: IllegalArgumentException) {
        null
    }

    open fun shouldUseOtelAgent(): Boolean {
        return isUserConfiguredDefaultInEnv() ||
                listOf(isMicronautTracing(), isSpringBootMicrometerTracing()).none { it }
    }

    open fun isSpringBootMicrometerTracing(): Boolean {
        if (isUserConfiguredDefaultInEnv()) return false
        return isUserConfiguredSpringBootMicrometerInEnv() || isSpringBootMicrometerTracing(resolvedModule, projectHeuristics)
    }

    open fun isMicronautTracing(): Boolean {
        if (isUserConfiguredDefaultInEnv()) return false
        return isUserConfiguredMicronautInEnv() || isMicronautTracing(resolvedModule, projectHeuristics)
    }

    private fun isSpringBootMicrometerTracing(module: Module?, projectHeuristics: ProjectHeuristics): Boolean {
        val isMicrometerTracingInSettings = isMicrometerTracingInSettings()
        val isSpringBootModule = isSpringBootModule(module)
        return isMicrometerTracingInSettings &&
                (isSpringBootModule || projectHeuristics.hasOnlySpringBootModules())

    }

    private fun isSpringBootModule(module: Module?): Boolean {
        val moduleMetadata = getModuleMetadata(module)
        return moduleMetadata?.hasSpringBoot() ?: false
    }

    private fun isMicronautTracing(module: Module?, projectHeuristics: ProjectHeuristics): Boolean {
        return isMicronautModule(module) || projectHeuristics.hasOnlyMicronautModules()
    }

    private fun isMicronautModule(module: Module?): Boolean {
        val moduleMetadata = getModuleMetadata(module)
        return moduleMetadata?.hasMicronaut() ?: false
    }


    fun isUserConfiguredSpringBootMicrometerInEnv(): Boolean {
        return flavorInEnv == Flavors.SpringBootMicrometer
    }

    fun isUserConfiguredMicronautInEnv(): Boolean {
        return flavorInEnv == Flavors.Micronaut
    }

    fun isUserConfiguredQuarkusInEnv(): Boolean {
        return flavorInEnv == Flavors.Quarkus
    }

    fun isUserConfiguredDefaultInEnv(): Boolean {
        return flavorInEnv == Flavors.Default
    }

}