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
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.buildsystem.JavaBuildSystem
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.jetbrains.idea.maven.execution.MavenRunConfiguration
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration

class OtelRunConfigurationExtension : RunConfigurationExtension() {

    private val logger: Logger = Logger.getInstance(OtelRunConfigurationExtension::class.java)


    private fun isAutoOtelEnabled(): Boolean {
        return PersistenceService.getInstance().isObservabilityEnabled()
    }

    private fun isConnectedToBackend(project: Project): Boolean {
        return BackendConnectionMonitor.getInstance(project).isConnectionOk()
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
        try {
            val resolvedModule = tryResolveModule(configuration, params)

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

            run {
                val taskNames = extractTasks(configuration)
                ActivityMonitor.getInstance(configuration.project)
                    .reportRunConfig(wrapper.getRunConfigType(configuration, resolvedModule).name, taskNames, isAutoOtelEnabled, isConnectedToBackend)
            }

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
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("OtelRunConfigurationExtension.updateJavaParameters", e)
        }
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
            getWrapperFor(configuration)?.isGradleConfiguration(configuration) == true
        ) {
            handler.addProcessListener(object : ProcessListener {
                //clean the settings after the process starts.
                //NOTE: do not clean when process ends because if user adds the variables while the process is running
                // it will remove the variables.
                override fun startNotified(event: ProcessEvent) {
                    cleanGradleSettingsAfterProcessStart(configuration as GradleRunConfiguration)
                }
            })
        }
    }

    private fun reportUnknownTasksToPosthog(configuration: RunConfigurationBase<*>) {
        val buildSystem = extractBuildSystem(configuration)

        if (buildSystem != JavaBuildSystem.UNKNOWN) {
            val buildSystemName = buildSystem.name.lowercase()
            val taskNames = extractTasks(configuration).toMutableSet()
            taskNames.removeAll(KNOWN_IRRELEVANT_TASKS)

            val activityMonitor = ActivityMonitor.getInstance(configuration.project)
            activityMonitor.reportUnknownTaskRunning(buildSystemName, taskNames, configuration.javaClass.name, configuration.type.displayName)
        }
    }

    private fun extractBuildSystem(configuration: RunConfigurationBase<*>): JavaBuildSystem {
        return when (configuration) {
            is GradleRunConfiguration -> JavaBuildSystem.GRADLE
            is MavenRunConfiguration -> JavaBuildSystem.MAVEN
            else -> JavaBuildSystem.UNKNOWN
        }
    }

}