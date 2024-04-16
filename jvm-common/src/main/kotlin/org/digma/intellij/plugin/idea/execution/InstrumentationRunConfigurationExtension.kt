package org.digma.intellij.plugin.idea.execution

import com.intellij.execution.Executor
import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.SimpleProgramParameters
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.execution.RunConfigurationInstrumentationHandler
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor


class InstrumentationRunConfigurationExtension : RunConfigurationExtension() {

    private val logger: Logger = Logger.getInstance(this::class.java)

    private fun isObservabilityEnabled(): Boolean {
        return PersistenceService.getInstance().isObservabilityEnabled()
    }

    private fun isConnectedToBackend(project: Project): Boolean {
        return BackendConnectionMonitor.getInstance(project).isConnectionOk()
    }

    override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean {
        // always return true , so could later log unknown configurations
        return true
    }

    private fun getHandlerForConfiguration(configuration: RunConfiguration): RunConfigurationInstrumentationHandler? {
        return RunConfigurationHandlersHolder.runConfigurationHandlers.find { it.isApplicableFor(configuration) }
    }

    override fun <T : RunConfigurationBase<*>?> updateJavaParameters(
        configuration: T & Any,
        params: JavaParameters,
        runnerSettings: RunnerSettings?
    ) {

        Log.log(
            logger::debug, "updateJavaParameters, project:{}, id:{}, name:{}, type:{}",
            configuration.project, configuration.id, configuration.name, configuration.type
        )

        try {

            if (isObservabilityEnabled() && isConnectedToBackend(configuration.project)) {

                val runConfigurationHandler = getHandlerForConfiguration(configuration)

                if (runConfigurationHandler == null) {
                    reportUnknownConfigurationType(configuration)
                } else {
                    val javaToolOptions = runConfigurationHandler.updateParameters(configuration, params, runnerSettings)
                    if (javaToolOptions != null) {
                        reportRunConfig(runConfigurationHandler, javaToolOptions, configuration, params)
                    } else {
                        reportUnhandledConfiguration(runConfigurationHandler, configuration, params)
                    }
                }
            }
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("${this::class.java.simpleName}.updateJavaParameters", e)
        }
    }


    private fun reportRunConfig(
        runConfigurationHandler: RunConfigurationInstrumentationHandler,
        javaToolOptions: String,
        configuration: RunConfiguration,
        params: SimpleProgramParameters
    ) {
        try {
            val runConfigDescription = runConfigurationHandler.getConfigurationDescription(configuration, params)
            val runConfigurationType = runConfigurationHandler.getConfigurationType(configuration)
            ActivityMonitor.getInstance(configuration.project)
                .reportRunConfig(
                    runConfigurationType.name,
                    runConfigDescription,
                    javaToolOptions,
                    isObservabilityEnabled(),
                    isConnectedToBackend(configuration.project)
                )


        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("${this::class.java.simpleName}.reportRunConfig", e)
        }
    }


    private fun reportUnknownConfigurationType(configuration: RunConfiguration) {
        try {
            ActivityMonitor.getInstance(configuration.project)
                .reportUnknownConfigurationType(configuration.javaClass.name, configuration.type.id, configuration.type.displayName)
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("${this::class.java.simpleName}.reportUnknownConfigurationType", e)
        }
    }


    private fun reportUnhandledConfiguration(
        handler: RunConfigurationInstrumentationHandler,
        configuration: RunConfiguration,
        params: SimpleProgramParameters
    ) {
        try {
            val desc = handler.getConfigurationDescription(configuration, params)
            val taskNames = handler.getTaskNames(configuration).toMutableSet()
            taskNames.removeAll { task ->
                //gradle tasks may start with ':'
                val toRemoveWithoutColon = if (task.startsWith(":") && task.length > 1) task.substring(1) else task
                KNOWN_IRRELEVANT_TASKS.contains(toRemoveWithoutColon) ||
                        KNOWN_IRRELEVANT_TASKS.any { task.endsWith(it) }
            }
            val buildSystem = handler.getBuildSystem(configuration)
            ActivityMonitor.getInstance(configuration.project)
                .reportUnhandledConfiguration(desc, buildSystem.name, taskNames, configuration.javaClass.name, configuration.type.displayName)
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("${this::class.java.simpleName}.reportUnhandledConfiguration", e)
        }
    }


    override fun decorate(
        console: ConsoleView,
        configuration: RunConfigurationBase<*>,
        executor: Executor,
    ): ConsoleView {

        if (isObservabilityEnabled() &&
            isConnectedToBackend(configuration.project)
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
        try {

            //find a handler that wants to handle this configuration and also that should clean after start
            val configurationInstrumentationHandler =
                RunConfigurationHandlersHolder.runConfigurationHandlers.find {
                    it.isApplicableFor(configuration) && it.shouldCleanConfigurationAfterStart(configuration)
                }

            //we need to clean gradle configuration from our JAVA_TOOL_OPTIONS
            if (isObservabilityEnabled() && configurationInstrumentationHandler != null) {
                handler.addProcessListener(object : ProcessListener {
                    //clean the settings after the process starts.
                    //NOTE: do not clean when process ends because if user adds the variables while the process is running
                    // it will remove the variables.
                    override fun startNotified(event: ProcessEvent) {
                        configurationInstrumentationHandler.cleanConfigurationAfterStart(configuration)
                    }
                })
            }
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("${this::class.java.simpleName}.attachToProcess", e)
        }
    }

}