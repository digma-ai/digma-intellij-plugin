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
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration

//
// this class is derived from AutoOtelAgentRunConfigurationExtension.
// consider to combine them together with abstract class
//
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

            RunConfigType.GradleTest,
            RunConfigType.GradleRun,
            -> {
                configuration as GradleRunConfiguration
                val javaToolOptions = buildJavaToolOptions(runConfigType.isTest)
                javaToolOptions?.let {
                    //TODO: handle gradle
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
        if (isGradleConfiguration(configuration)) return RunConfigType.GradleRun
        if (isGradleTestConfiguration(configuration)) return RunConfigType.GradleTest
        return RunConfigType.Unknown
    }

    /**
     * @see <a href="https://github.com/OpenLiberty/ci.gradle/tree/main#tasks">Liberty Gradle Tasks</a>
     */
    override fun isGradleConfiguration(configuration: RunConfigurationBase<*>): Boolean {
        if (configuration is GradleRunConfiguration) {
            val taskNames = configuration.settings.taskNames
            val hasRelevantTask = taskNames.any {
                false
                        || it.contains("libertyDev")
                        || it.contains("libertyRun")
            }
            return hasRelevantTask
        }
        return false
    }

    private fun isGradleTestConfiguration(configuration: RunConfigurationBase<*>): Boolean {
        // currently no special task for liberty test
        return false
    }

    private fun isMavenConfiguration(configuration: RunConfigurationBase<*>): Boolean {
        //will catch maven tasks of liberty:dev and liberty:run
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
        //will catch maven tasks liberty:test-start
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