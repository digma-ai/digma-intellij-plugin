package org.digma.intellij.plugin.idea.execution

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
import org.digma.intellij.plugin.execution.RunConfigurationInstrumentationHandler
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor


class InstrumentationRunConfigurationExtension : RunConfigurationExtension() {

    private val logger: Logger = Logger.getInstance(this::class.java)

    //assign a local member for comfortability
    private val runConfigurationHandlers = RunConfigurationHandlersHolder.runConfigurationHandlers

    private fun isObservabilityEnabled(): Boolean {
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
        runnerSettings: RunnerSettings?
    ) {

        Log.log(
            logger::debug, "updateJavaParameters, project:{}, id:{}, name:{}, type:{}",
            configuration.project, configuration.id, configuration.name, configuration.type
        )

        try {

            if (isObservabilityEnabled() && isConnectedToBackend(configuration.project)) {

                val runConfigurationHandler = getHandlerForConfiguration(configuration, params)

                if (runConfigurationHandler == null) {
                    reportUnhandledConfiguration(configuration, params)
                } else {

                    val runConfigDescription = runConfigurationHandler.getConfigurationDescription(configuration)
                    val runConfigurationType = runConfigurationHandler.getConfigurationType(configuration, params)
                    ActivityMonitor.getInstance(configuration.project)
                        .reportRunConfig(
                            runConfigurationType.name,
                            runConfigDescription,
                            isObservabilityEnabled(),
                            isConnectedToBackend(configuration.project)
                        )

                    runConfigurationHandler.updateParameters(configuration, params, runnerSettings)
                }
            }
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("InstrumentationRunConfigurationExtension.updateJavaParameters", e)
        }
    }


    private fun getHandlerForConfiguration(configuration: RunConfigurationBase<*>, params: JavaParameters): RunConfigurationInstrumentationHandler? {
        return runConfigurationHandlers.find { it.isApplicableFor(configuration, params) }
    }


    private fun reportUnhandledConfiguration(configuration: RunConfigurationBase<*>, params: JavaParameters) {

        //this should only be used to find a handler that can handle this configuration and help
        // extract details from the configuration for reporting an unhandled configuration
        fun getHandlerForConfigurationTypeSorted(configuration: RunConfigurationBase<*>): RunConfigurationInstrumentationHandler? {
            return runConfigurationHandlers.sortedBy { it.getOrder() }
                .find { it.isHandlingType(configuration) }
        }

        val handler = getHandlerForConfigurationTypeSorted(configuration)

        if (handler == null) {
            ActivityMonitor.getInstance(configuration.project)
                .reportUnknownConfiguration(configuration.javaClass.name, configuration.type.displayName)
        } else {
            val desc = handler.getConfigurationDescription(configuration)
            val taskNames = handler.getTaskNames(configuration).toMutableSet()
            taskNames.removeAll(KNOWN_IRRELEVANT_TASKS)
            val buildSystem = handler.getBuildSystem(configuration)
            ActivityMonitor.getInstance(configuration.project)
                .reportUnhandledConfiguration(desc, buildSystem.name, taskNames, configuration.javaClass.name, configuration.type.displayName)
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
            val configurationInstrumentationHandler = runConfigurationHandlers.find {
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
            ErrorReporter.getInstance().reportError("OtelRunConfigurationExtension.attachToProcess", e)
        }
    }

}