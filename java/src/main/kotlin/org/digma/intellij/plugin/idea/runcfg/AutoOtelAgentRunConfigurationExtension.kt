package org.digma.intellij.plugin.idea.runcfg

import com.intellij.execution.CommonJavaRunConfigurationParameters
import com.intellij.execution.Executor
import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.ModuleRunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.diagnostic.Logger
import kotlinx.collections.immutable.toImmutableMap
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.settings.SettingsState
import org.jetbrains.idea.maven.execution.MavenRunConfiguration
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration


class AutoOtelAgentRunConfigurationExtension : RunConfigurationExtension() {

    private val logger: Logger = Logger.getInstance(AutoOtelAgentRunConfigurationExtension::class.java)

    private fun enabled(): Boolean {
        return PersistenceService.getInstance().state.isAutoOtel
    }

    /*
    Note about gradle
    in intellij the user can configure to run main method or unit tests with gradle or with
    intellij. it's in gradle settings.
    usually gradle is the default. if gradle is selected, the gradle plugin will wrap a main method
    run configuration with a gradle script generated in place and a JavaExec task that runs the main method.
    unit tests will be executed by calling gradle :test task.
     */


    override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean {
        Log.log(logger::debug, "isApplicableFor, project:{}, id:{}, name:{}, type:{}",
                configuration.project, configuration.id, configuration.name, configuration.type)

        if (isJavaConfiguration(configuration) || isGradleConfiguration(configuration) || isMavenConfiguration(configuration)) {
            return true
        }

        return false
    }


    override fun <T : RunConfigurationBase<*>?> updateJavaParameters(
            configuration: T & Any,
            params: JavaParameters,
            runnerSettings: RunnerSettings?
    ) {

        Log.log(logger::debug, "updateJavaParameters, project:{}, id:{}, name:{}, type:{}",
                configuration.project, configuration.id, configuration.name, configuration.type)

        //testing if enabled must be done here just before running.
        if (!enabled()) {
            Log.log(logger::debug, "AutoOtelAgentRunConfigurationExtension is not enabled")
            return
        }

        if (isJavaConfiguration(configuration)) {
            configuration as CommonJavaRunConfigurationParameters

            adjustWithOtelAndDigma(configuration, params)
        } else if (isGradleConfiguration(configuration)) {
            configuration as GradleRunConfiguration

            fillPropertiesAndEnvIfNeeded(params, configuration)

            adjustWithOtelAndDigma(configuration, params)

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

        } else if (isMavenConfiguration(configuration)) {
            configuration as MavenRunConfiguration

            adjustWithOtelAndDigma(configuration, params)
        }
    }

    /**
     * temp method and is used so JavaParameters would be ready for next call, which check for existent systemProperty or environmentVariable
     * apparently for gradle the systemProperty and environmentVariable are not set during the hook of updateXxx.
     * opened an issue to track it https://youtrack.jetbrains.com/issue/IDEA-317118
     * so once issue is solved can remove this method
     */
    private fun fillPropertiesAndEnvIfNeeded(params: JavaParameters, configuration: GradleRunConfiguration) {
        if (params.env.isNullOrEmpty()) {
            if (!configuration.settings.env.isNullOrEmpty()) {
                params.env = configuration.settings.env.toImmutableMap()
            }
        }
        if (params.vmParametersList.parametersCount <= 0) {
            if (!configuration.settings.vmOptions.isNullOrBlank()) {
                params.vmParametersList.addParametersString(configuration.settings.vmOptions)
            }
        }
    }

    override fun decorate(
            console: ConsoleView,
            configuration: RunConfigurationBase<*>,
            executor: Executor
    ): ConsoleView {

        if (enabled()) {
            console.print("Observability - this process is enhanced by Digma OTEL agent\n", ConsoleViewContentType.LOG_WARNING_OUTPUT)
        }

        return console
    }

    private fun adjustWithOtelAndDigma(configuration: RunConfigurationBase<*>, params: JavaParameters) {

        val otelAgentPath = OTELJarProvider.getInstance().getOtelAgentJarPath(configuration.project)
        val digmaExtensionPath = OTELJarProvider.getInstance().getDigmaAgentExtensionJarPath(configuration.project)

        if (otelAgentPath == null || digmaExtensionPath == null) {
            Log.log(logger::debug, "could not adjust process because OTEL agent and Digma extension are not available. please check the logs")
            return
        }

        params.vmParametersList.addParametersString("-javaagent:$otelAgentPath")
        params.vmParametersList.addProperty("otel.javaagent.extensions", digmaExtensionPath)
        params.vmParametersList.addProperty("otel.traces.exporter", "otlp")
        params.vmParametersList.addProperty("otel.metrics.exporter", "none")
        params.vmParametersList.addProperty("otel.exporter.otlp.traces.endpoint", getExporterUrl())

        if (!isOtelServiceNameAlreadyDefined(params)) {
            params.vmParametersList.addProperty("otel.service.name", evalServiceName(configuration))
        }
    }

    private fun isOtelEntryDefined(javaConfParams: JavaParameters, environmentVariable: String, systemProperty: String): Boolean {
        return javaConfParams.env.containsKey(environmentVariable)
                || isVmEntryExists(javaConfParams, systemProperty)
    }

    /**
     * see <a href="https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md#opentelemetry-resource"></a>
     */
    private fun isOtelServiceNameAlreadyDefined(javaConfParams: JavaParameters): Boolean {
        return isOtelEntryDefined(javaConfParams, "OTEL_SERVICE_NAME", "otel.service.name")
    }

    private fun isVmEntryExists(javaConfParams: JavaParameters, entryName: String): Boolean {
        return javaConfParams.vmParametersList.hasProperty(entryName)
    }

    private fun evalServiceName(configuration: RunConfigurationBase<*>): String {
        return if (configuration is ModuleRunConfiguration && configuration.modules.isNotEmpty()) {
            val moduleName = configuration.modules.first().name
            moduleName.replace(" ", "").trim()
        } else {
            configuration.project.name.replace(" ", "").trim()
        }
    }

    private fun getExporterUrl(): String {
        return SettingsState.getInstance().runtimeObservabilityBackendUrl
    }


    private fun isJavaConfiguration(configuration: RunConfigurationBase<*>): Boolean {
        /*
        this will catch the following run configuration types:
        ApplicationConfiguration,JUnitConfiguration,TestNGConfiguration,KotlinRunConfiguration,
        KotlinStandaloneScriptRunConfiguration,GroovyScriptRunConfiguration,JarApplicationConfiguration
         */
        return configuration is CommonJavaRunConfigurationParameters
    }


    private fun isGradleConfiguration(configuration: RunConfigurationBase<*>): Boolean {
        /*
        this will catch gradle running a main method or a unit test or spring bootRun.
        all other gradle tasks are ignored.
         */
        if (configuration is GradleRunConfiguration) {
            val taskNames = configuration.settings.taskNames
            val isMainMethod = taskNames.any {
                it.contains(".main")
            }
            val hasTestTask = taskNames.any {
                it.contains(":test") || it.equals("test")
            }
            val hasBootRun = taskNames.any {
                it.contains(":bootRun") || it.equals("bootRun")
            }
            if (isMainMethod || hasTestTask || hasBootRun) {
                return true
            }
        }

        return false
    }


    private fun isMavenConfiguration(configuration: RunConfigurationBase<*>): Boolean {
        //will catch maven exec plugin goals, bootRun and uni tests
        if (configuration is MavenRunConfiguration) {
            val goals = configuration.runnerParameters.goals
            val isExecExecJava = goals.contains("exec:exec") && goals.contains("-Dexec.executable=java")
            if (isExecExecJava ||
                    goals.contains("spring-boot:run") ||
                    goals.contains("exec:java") ||
                    goals.contains("surefire:test")) {
                return true
            }
        }
        return false
    }
}