package org.digma.intellij.plugin.idea.runcfg

import com.intellij.execution.Executor
import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.jetbrains.idea.maven.execution.MavenRunConfiguration
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration

class OtelRunConfigurationExtension : RunConfigurationExtension() {

    companion object {
        val logger: Logger = Logger.getInstance(OtelRunConfigurationExtension::class.java)
        const val ORG_GRADLE_JAVA_TOOL_OPTIONS = "ORG_GRADLE_JAVA_TOOL_OPTIONS"
    }

    private fun isAutoOtelEnabled(): Boolean {
        return PersistenceService.getInstance().state.isAutoOtel
    }

    private fun isConnectedToBackend(project: Project): Boolean {
        return BackendConnectionMonitor.getInstance(project).isConnectionOk()
    }

    private fun getWrapperFor(configuration: RunConfigurationBase<*>, module: Module?): IRunConfigurationWrapper? {
        return listOf(
            QuarkusRunConfigurationWrapper.getInstance(configuration.project), // quarkus is first so could catch unit tests before the standard AutoOtelAgent
            AutoOtelAgentRunConfigurationWrapper.getInstance(configuration.project),
        ).firstOrNull {
            it.canWrap(configuration, module)
        }
    }

    // not the ideal call but might manage without the JavaParameters
    private fun getWrapperFor(configuration: RunConfigurationBase<*>): IRunConfigurationWrapper? {
        val module = RunCfgTools.tryResolveModule(configuration, null)
        return getWrapperFor(configuration, module)
    }

    override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean {
        // always return true , so could later log unknown tasks
        return true
    }

    override fun <T : RunConfigurationBase<*>?> updateJavaParameters(
        configuration: T & Any,
        params: JavaParameters,
        runnerSettings: RunnerSettings?,
    ) {
        val resolvedModule = RunCfgTools.tryResolveModule(configuration, params)

        val wrapper = getWrapperFor(configuration, resolvedModule)
        if (wrapper == null) {
            reportUnknownTasksToPosthog(configuration)
            return
        }

        val isAutoOtelEnabled = isAutoOtelEnabled()
        val isConnectedToBackend = isConnectedToBackend(configuration.project)

        Log.log(
            logger::debug, "updateJavaParameters, project:{}, id:{}, name:{}, type:{}, module: {}",
            configuration.project, configuration.id, configuration.name, configuration.type, resolvedModule
        )

        ActivityMonitor
            .getInstance(configuration.project)
            .reportRunConfig(wrapper.getRunConfigType(configuration, resolvedModule).name, isAutoOtelEnabled, isConnectedToBackend)

        //testing if enabled must be done here just before running.
        if (!isAutoOtelEnabled) {
            Log.log(logger::debug, "autoInstrumentation is not enabled")
            return
        }

        if (!isConnectedToBackend) {
            Log.log(logger::warn, "No connection to Digma backend. Otel won't be exported")
            return
        }

        wrapper.updateJavaParameters(configuration, params, runnerSettings, resolvedModule)
    }

    override fun decorate(
        console: ConsoleView,
        configuration: RunConfigurationBase<*>,
        executor: Executor,
    ): ConsoleView {
        if (isAutoOtelEnabled() &&
            isConnectedToBackend(configuration.project) &&
            getWrapperFor(configuration) != null
        ) {
            //that only works for java and maven run configurations.
            console.print("This process is enhanced by Digma!\n", ConsoleViewContentType.LOG_WARNING_OUTPUT)
        }
        return console
    }

    override fun attachToProcess(
        configuration: RunConfigurationBase<*>,
        handler: ProcessHandler,
        runnerSettings: RunnerSettings?,
    ) {
        //we need to clean gradle configuration from our JAVA_TOOL_OPTIONS
        if (isAutoOtelEnabled() &&
            isConnectedToBackend(configuration.project) &&
            getWrapperFor(configuration)?.isGradleConfiguration(configuration) == true
        ) {
            handler.addProcessListener(object : ProcessListener {

                override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {
                    cleanGradleSettings(configuration)
                }

                private fun cleanGradleSettings(configuration: RunConfigurationBase<*>) {
                    configuration as GradleRunConfiguration
                    Log.log(logger::debug, "Cleaning gradle configuration {}", configuration)
                    if (configuration.settings.env.containsKey(ORG_GRADLE_JAVA_TOOL_OPTIONS)) {
                        val orgJavaToolOptions = configuration.settings.env[ORG_GRADLE_JAVA_TOOL_OPTIONS]
                        configuration.settings.env[JAVA_TOOL_OPTIONS] = orgJavaToolOptions
                        configuration.settings.env.remove(ORG_GRADLE_JAVA_TOOL_OPTIONS)
                    } else if (configuration.settings.env.containsKey(JAVA_TOOL_OPTIONS)) {
                        configuration.settings.env.remove(JAVA_TOOL_OPTIONS)
                    }
                }
            })
        }
    }

    private fun reportUnknownTasksToPosthog(configuration: RunConfigurationBase<*>) {
        val activityMonitor = ActivityMonitor.getInstance(configuration.project)

        if (configuration is GradleRunConfiguration) {
            val taskNames = configuration.settings.taskNames.toMutableSet()
            taskNames.removeAll(KNOWN_IRRELEVANT_TASKS)
            activityMonitor.reportUnknownTaskRunning("gradle", taskNames, configuration.javaClass.name, configuration.type.displayName)
        }

        if (configuration is MavenRunConfiguration) {
            val goals = configuration.runnerParameters.goals.toMutableSet()
            goals.removeAll(KNOWN_IRRELEVANT_TASKS)
            activityMonitor.reportUnknownTaskRunning("maven", goals, configuration.javaClass.name, configuration.type.displayName)
        }
    }
}