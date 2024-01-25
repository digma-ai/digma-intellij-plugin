package org.digma.intellij.plugin.idea.runcfg

import com.intellij.execution.JavaTestConfigurationBase
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.common.buildEnvForLocalTests
import org.digma.intellij.plugin.idea.deps.ModulesDepsService
import org.digma.intellij.plugin.settings.SettingsState
import org.jetbrains.idea.maven.execution.MavenRunConfiguration
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration

//
// this class is derived from AutoOtelAgentRunConfigurationExtension.
// consider to combine them together with abstract class
//
// for quarkus, look at https://quarkus.io/guides/opentelemetry
class QuarkusRunConfigurationWrapper : RunConfigurationWrapper {

    private val logger: Logger = Logger.getInstance(QuarkusRunConfigurationWrapper::class.java)

    companion object {
        @JvmStatic
        fun getInstance(project: Project): QuarkusRunConfigurationWrapper {
            return project.getService(QuarkusRunConfigurationWrapper::class.java)
        }
    }

    override fun getRunConfigType(configuration: RunConfiguration, module: Module?): RunConfigType {
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
        when (val runConfigType = evalRunConfigType(configuration, resolvedModule)) {
            RunConfigType.GradleRun -> {
                //when injecting JAVA_TOOL_OPTIONS to GradleRunConfiguration the GradleRunConfiguration will also run with
                // JAVA_TOOL_OPTIONS which is not ideal. for example, gradle will execute with JAVA_TOOL_OPTIONS and then fork
                // a process for the main method or unit test with JAVA_TOOL_OPTIONS. ideally we want only the forked process
                // to run with JAVA_TOOL_OPTIONS. if that causes any issues then we need a way to change the script
                // that gradle generates instead of using JAVA_TOOL_OPTIONS.
                //for example, gradle takes a run configuration for running a main method and generates a gradle script with
                // a JavaExec task and runs the task. the best would be to add jvmArgs to this task.
                // to learn how gradle does it see GradleTaskManager and GradleExecutionHelper.
                //when gradle runs the :test task it's not possible to pass system properties to the task.
                // JAVA_TOOL_OPTIONS is not the best as said above, but it works.
                configuration as GradleRunConfiguration
                val javaToolOptions = buildJavaToolOptions(runConfigType.isTest)
                mergeGradleJavaToolOptions(configuration, javaToolOptions)
            }

            RunConfigType.MavenTest,
            RunConfigType.MavenRun,
            -> {
                configuration as MavenRunConfiguration
                val javaToolOptions = buildJavaToolOptions(runConfigType.isTest)
                mergeJavaToolOptions(configuration.project, params, javaToolOptions)
            }

            RunConfigType.JavaTest -> {
                val javaToolOptions = buildJavaToolOptions(runConfigType.isTest)
                mergeJavaToolOptions(configuration.project, params, javaToolOptions)
            }

            else -> {
                // do nothing
            }
        } // end when case
    }


    /**
     * @see <a href="https://quarkus.io/guides/opentelemetry">Quarkus with opentelemetry</a>
     */
    private fun buildJavaToolOptions(isTest: Boolean): String {
        var retVal = " "
            .plus("-Dquarkus.otel.exporter.otlp.traces.endpoint=${getExporterUrl()}")
            .plus(" ")

        if (isTest) {
            val envPart = "digma.environment=${buildEnvForLocalTests()}"
            retVal = retVal
                .plus("-Dquarkus.otel.resource.attributes=\"$envPart\"")
                .plus(" ")

            // handle batching - try to remove them, so spans would manage to be sent out
            retVal = retVal
                .plus("-Dquarkus.otel.bsp.schedule.delay=0.001S") // set delay to 1 millisecond
                .plus(" ")
                .plus("-Dquarkus.otel.bsp.max.export.batch.size=1") // by setting size of 1 it kind of disable
                .plus(" ")
        }

        return retVal
    }

    private fun getExporterUrl(): String {
        return SettingsState.getInstance().runtimeObservabilityBackendUrl
    }

    private fun isQuarkusModule(module: Module?): Boolean {
        if (module == null) return false

        val modulesDepsService = ModulesDepsService.getInstance(module.project)
        return modulesDepsService.isQuarkusModule(module)
    }

    private fun evalRunConfigType(configuration: RunConfiguration, module: Module?): RunConfigType {
        if (isGradleConfiguration(configuration)) return RunConfigType.GradleRun
        if (isMavenConfiguration(configuration)) return RunConfigType.MavenRun
        if (isMavenTestConfiguration(configuration)) return RunConfigType.MavenTest
        if (isJavaTest(configuration, module)) return RunConfigType.JavaTest
        return RunConfigType.Unknown
    }

    /**
     * @see <a href="https://quarkus.io/guides/opentelemetry">Quarkus with opentelemetry</a>
     */
    override fun isGradleConfiguration(configuration: RunConfiguration): Boolean {
        //TODO: support Quarkus with gradle
        return false
    }

    /**
     * @see <a href="https://quarkus.io/guides/quarkus-maven-plugin">Quarkus and maven tasks</a>
     * @see <a href="https://quarkus.io/guides/opentelemetry">Quarkus with opentelemetry</a>
     */
    private fun isMavenConfiguration(configuration: RunConfiguration): Boolean {
        //will catch maven tasks of quarkus:dev and quarkus:run
        if (configuration is MavenRunConfiguration) {
            val goalNames = configuration.runnerParameters.goals
            val hasRelevantTask = goalNames.any {
                false
                        || it.equals("quarkus:dev")
                        || it.equals("quarkus:run")
                        || (it.contains(":quarkus-maven-plugin:") && it.endsWith(":dev"))
                        || (it.contains(":quarkus-maven-plugin:") && it.endsWith(":run"))
            }
            return hasRelevantTask
        }
        return false
    }

    private fun isMavenTestConfiguration(configuration: RunConfiguration): Boolean {
        //will catch maven tasks quarkus:test
        if (configuration is MavenRunConfiguration) {
            val goalNames = configuration.runnerParameters.goals
            val hasRelevantTask = goalNames.any {
                false
                        || it.equals("quarkus:test")
                        || (it.contains(":quarkus-maven-plugin:") && it.endsWith(":test"))
            }
            return hasRelevantTask
        }
        return false
    }

    private fun isJavaTest(configuration: RunConfiguration, module: Module?): Boolean {
        if (configuration is JavaTestConfigurationBase) {
            if (isQuarkusModule(module)) {
                return true
            }
        }
        return false
    }

}