package org.digma.intellij.plugin.idea.runcfg

import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.common.buildEnvForLocalTests
import org.digma.intellij.plugin.settings.SettingsState
import org.jetbrains.idea.maven.execution.MavenRunConfiguration

//
// this class is derived from AutoOtelAgentRunConfigurationExtension.
// consider to combine them together with abstract class
//
// for quarkus, look at https://quarkus.io/guides/opentelemetry
class OpenLibertyRunConfigurationWrapper : IRunConfigurationWrapper {

    companion object {
        val logger: Logger = Logger.getInstance(OpenLibertyRunConfigurationWrapper::class.java)

        @JvmStatic
        fun getInstance(project: Project): OpenLibertyRunConfigurationWrapper {
            return project.getService(OpenLibertyRunConfigurationWrapper::class.java)
        }
    }

    override fun getRunConfigType(configuration: RunConfigurationBase<*>, module: Module?): RunConfigType {
        return evalRunConfigType(configuration, module)
    }

    /*
    Note about gradle
    in intellij the user can configure to run main method or unit tests with gradle or with
    intellij. it's in gradle settings.
    usually gradle is the default. if gradle is selected, the gradle plugin will wrap a main method
    run configuration with a gradle script generated in place and a JavaExec task that runs the main method.
    unit tests will be executed by calling gradle :test task.
     */

    override fun <T : RunConfigurationBase<*>?> updateJavaParameters(
        configuration: T & Any,
        params: JavaParameters,
        runnerSettings: RunnerSettings?,
        resolvedModule: Module?,
    ) {
        val runConfigType = evalRunConfigType(configuration, resolvedModule)
        when (runConfigType) {
            RunConfigType.MavenTest,
            RunConfigType.MavenRun,
            -> {
                configuration as MavenRunConfiguration
                val javaToolOptions = buildJavaToolOptions(runConfigType.isTest)
                javaToolOptions?.let {
                    mergeJavaToolOptions(params, it)
                }
            }

            else -> {
                // do nothing
            }
        } // end when case
    }

    //this is for java and maven run configurations. merge in case users have their own JAVA_TOOL_OPTIONS
    private fun mergeJavaToolOptions(params: JavaParameters, myJavaToolOptions: String) {
        var javaToolOptions = myJavaToolOptions
        if (params.env.containsKey(JAVA_TOOL_OPTIONS)) {
            val currentJavaToolOptions = params.env[JAVA_TOOL_OPTIONS]
            javaToolOptions = "$myJavaToolOptions $currentJavaToolOptions"
        }
        params.env[JAVA_TOOL_OPTIONS] = javaToolOptions
    }

    /**
     * @see <a href="https://openliberty.io/docs/latest/microprofile-telemetry.html">OpenLiberty - Enable distributed tracing with MicroProfile Telemetry</a>
     */
    private fun buildJavaToolOptions(isTest: Boolean): String {
        var retVal = " "
            .plus("-Dotel.sdk.disabled=false") // in order to enable MicroProfile Telemetry, need to disable otel sdk
            .plus(" ")
            .plus("-Dotel.exporter.otlp.endpoint=${getExporterUrl()}")
            .plus(" ")

        if (isTest) {
            val envPart = "digma.environment=${buildEnvForLocalTests()}"
            retVal = retVal
                .plus("-Dotel.resource.attributes=\"$envPart\"")
                .plus(" ")
        }

        return retVal
    }

    private fun getExporterUrl(): String {
        return SettingsState.getInstance().runtimeObservabilityBackendUrl
    }

    private fun evalRunConfigType(configuration: RunConfigurationBase<*>, module: Module?): RunConfigType {
        if (isMavenConfiguration(configuration)) return RunConfigType.MavenRun
        if (isMavenTestConfiguration(configuration)) return RunConfigType.MavenTest
        return RunConfigType.Unknown
    }

    /**
     * @see <a href="https://quarkus.io/guides/opentelemetry">Quarkus with opentelemetry</a>
     */
    override fun isGradleConfiguration(configuration: RunConfigurationBase<*>): Boolean {
        //TODO: support OpenLiberty with gradle
        return false
    }

    private fun isMavenConfiguration(configuration: RunConfigurationBase<*>): Boolean {
        //will catch maven tasks of quarkus:dev and quarkus:run
        if (configuration is MavenRunConfiguration) {
            val goalNames = configuration.runnerParameters.goals
            val hasRelevantTask = goalNames.any {
                false
                        || it.equals("liberty:dev")
                        || it.equals("liberty:run")
                        || (it.contains(":liberty-maven-plugin:") && it.endsWith(":dev"))
                        || (it.contains(":liberty-maven-plugin:") && it.endsWith(":run"))
            }
            return hasRelevantTask
        }
        return false
    }

    private fun isMavenTestConfiguration(configuration: RunConfigurationBase<*>): Boolean {
        //will catch maven tasks quarkus:test
        if (configuration is MavenRunConfiguration) {
            val goalNames = configuration.runnerParameters.goals
            val hasRelevantTask = goalNames.any {
                false
                        || it.equals("liberty:test-start")
                        || (it.contains(":liberty-maven-plugin:") && it.endsWith(":test-start"))
            }
            return hasRelevantTask
        }
        return false
    }

}