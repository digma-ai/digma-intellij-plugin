package org.digma.intellij.plugin.idea.execution.flavor

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.SimpleProgramParameters
import org.digma.intellij.plugin.execution.RunConfigurationInstrumentationService
import org.digma.intellij.plugin.idea.execution.JavaToolOptionsBuilder
import org.digma.intellij.plugin.idea.execution.ModuleResolver
import org.digma.intellij.plugin.idea.execution.ParametersExtractor
import org.digma.intellij.plugin.idea.execution.ProjectHeuristics
import org.digma.intellij.plugin.idea.execution.ServiceNameProvider


const val INSTRUMENTATION_FLAVOR_ENV_NAME = "INSTRUMENTATION_FLAVOR"

enum class Flavor { Default, Micronaut, Quarkus, SpringBootMicrometer, OpenLiberty, JavaServer }

/*
    todo and known issues:
      * when the project is setup to spring boot and micrometer and running test from intellij
        the selected flavor is SpringBootMicrometer but it doesn't produce data. changing to
        INSTRUMENTATION_FLAVOR=Default works.
      * a test annotated with @MicronautTest does not produce test data
 */

interface InstrumentationFlavor {


    companion object {

        //sort is important
        private val instrumentationFlavors = listOf(
            QuarkusInstrumentationFlavor(),
            MicronautInstrumentationFlavor(),
            SpringBootMicrometerInstrumentationFlavor(),
            JavaServerInstrumentationFlavor(),
            OpenLibertyInstrumentationFlavor(),
            DefaultInstrumentationFlavor()
        ).sortedBy { it.getOrder() }

        /**
         * returns an InstrumentationFlavor for this configuration.
         * or null if there is no InstrumentationFlavor that can handle this configuration.
         */
        fun get(
            instrumentationService: RunConfigurationInstrumentationService,
            configuration: RunConfiguration,
            params: SimpleProgramParameters,
            runnerSettings: RunnerSettings?,
            projectHeuristics: ProjectHeuristics,
            moduleResolver: ModuleResolver,
            parametersExtractor: ParametersExtractor
        ): InstrumentationFlavor? {

            val flavor = getFlavorInEnv(parametersExtractor)

            return instrumentationFlavors.firstOrNull {
                it.accept(
                    flavor,
                    instrumentationService,
                    configuration,
                    params,
                    runnerSettings,
                    projectHeuristics,
                    moduleResolver,
                    parametersExtractor
                )
            }
        }


        private fun getFlavorInEnv(parametersExtractor: ParametersExtractor): Flavor? {
            return try {
                val flavorValueInEnv = parametersExtractor.extractEnvValue(INSTRUMENTATION_FLAVOR_ENV_NAME)
                flavorValueInEnv?.let {
                    Flavor.valueOf(it)
                }
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }


    /**
     * the order in which accept method is invoked to select a flavor.
     * it is necessary because in some cases more than one flavor may be applicable for a configuration.
     * for example: both Quarkus and Default are applicable when running a quarkus app using intellij ApplicationConfiguration,
     * but if it's a quarkus module we want the quarkus to be selected.
     * so making quarkus higher order than default will select quarkus.
     * there are not so many cases, the above is the first and currently the only one we know of.
     * some flavors are the same higher order, but they should not accept the same configuration.
     * DefaultInstrumentationFlavor should be the lowest order
     */
    fun getOrder(): Int

    fun getPreferredUserFlavor(): Flavor

    fun accept(
        //this is flavor configured by the user in env variable
        userFlavor: Flavor?,
        instrumentationService: RunConfigurationInstrumentationService,
        configuration: RunConfiguration,
        params: SimpleProgramParameters,
        runnerSettings: RunnerSettings?,
        projectHeuristics: ProjectHeuristics,
        moduleResolver: ModuleResolver,
        parametersExtractor: ParametersExtractor
    ): Boolean

    /**
     * build the java tool options.
     * may still decide not to build it and return null.
     * or will return null in case of errors.
     */
    fun buildJavaToolOptions(
        instrumentationService: RunConfigurationInstrumentationService,
        javaToolOptionsBuilder: JavaToolOptionsBuilder,
        configuration: RunConfiguration,
        params: SimpleProgramParameters,
        runnerSettings: RunnerSettings?,
        projectHeuristics: ProjectHeuristics,
        moduleResolver: ModuleResolver,
        parametersExtractor: ParametersExtractor,
        serviceNameProvider: ServiceNameProvider
    ): String?


}