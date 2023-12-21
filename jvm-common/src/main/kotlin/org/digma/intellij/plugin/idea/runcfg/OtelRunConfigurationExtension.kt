package org.digma.intellij.plugin.idea.runcfg

import com.intellij.execution.Executor
import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfiguration
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
import org.digma.intellij.plugin.idea.buildsystem.JavaBuildSystem
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.jetbrains.idea.maven.execution.MavenRunConfiguration
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration

class OtelRunConfigurationExtension : RunConfigurationExtension() {

    companion object {
        val logger: Logger = Logger.getInstance(OtelRunConfigurationExtension::class.java)
        const val ORG_GRADLE_JAVA_TOOL_OPTIONS = "ORG_GRADLE_JAVA_TOOL_OPTIONS"


        //this is for java and maven run configurations. merge in case users have their own JAVA_TOOL_OPTIONS
        @JvmStatic
        fun mergeJavaToolOptions(params: JavaParameters, myJavaToolOptions: String) {
            var javaToolOptions = myJavaToolOptions
            if (params.env.containsKey(JAVA_TOOL_OPTIONS)) {
                val currentJavaToolOptions = params.env[JAVA_TOOL_OPTIONS]
                javaToolOptions = "$myJavaToolOptions $currentJavaToolOptions"
            }
            params.env[JAVA_TOOL_OPTIONS] = javaToolOptions
        }

        //this is only for gradle. we need to keep original JAVA_TOOL_OPTIONS if exists and restore when the process is
        // finished, anyway we need to clean our JAVA_TOOL_OPTIONS because it will be saved in the run configuration settings.
        @JvmStatic
        fun mergeGradleJavaToolOptions(configuration: GradleRunConfiguration, myJavaToolOptions: String) {
            var javaToolOptions = myJavaToolOptions
            //need to replace the env because it may be immutable map
            val newEnv = configuration.settings.env.toMutableMap()
            if (configuration.settings.env.containsKey(JAVA_TOOL_OPTIONS)) {
                val currentJavaToolOptions = configuration.settings.env[JAVA_TOOL_OPTIONS]
                javaToolOptions = "$myJavaToolOptions $currentJavaToolOptions"
                newEnv[ORG_GRADLE_JAVA_TOOL_OPTIONS] = currentJavaToolOptions!!
            }
            newEnv[JAVA_TOOL_OPTIONS] = javaToolOptions
            configuration.settings.env = newEnv
        }



        fun isAutoOtelEnabled(): Boolean {
            return PersistenceService.getInstance().state.isAutoOtel
        }

        fun getWrapperFor(configuration: RunConfiguration, module: Module?): RunConfigurationWrapper? {
            return listOf(
                QuarkusRunConfigurationWrapper.getInstance(configuration.project), // quarkus is first so could catch unit tests before the standard AutoOtelAgent
                OpenLibertyRunConfigurationWrapper.getInstance(configuration.project),
                AutoOtelAgentRunConfigurationWrapper.getInstance(configuration.project),
                EeAppServerAtIdeaUltimateRunConfigurationWrapper.getInstance(configuration.project),
                TomcatRunConfigurationWrapperForIdeaUltimate.getInstance(configuration.project),
            ).firstOrNull {
                it.canWrap(configuration, module)
            }
        }

        // not the ideal call but might manage without the JavaParameters
        fun getWrapperFor(configuration: RunConfiguration): RunConfigurationWrapper? {
            val module = RunCfgTools.tryResolveModule(configuration, null)
            return getWrapperFor(configuration, module)
        }
    }

    private fun isAutoOtelEnabled(): Boolean {
        return PersistenceService.getInstance().state.isAutoOtel
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

        run {
            val taskNames = RunCfgTools.extractTasks(configuration)
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
                        val newEnv = mutableMapOf<String, String>()
                        val orgJavaToolOptions = configuration.settings.env[ORG_GRADLE_JAVA_TOOL_OPTIONS]
                        newEnv.putAll(configuration.settings.env)
                        if (orgJavaToolOptions != null) {
                            newEnv[JAVA_TOOL_OPTIONS] = orgJavaToolOptions
                        }
                        newEnv.remove(ORG_GRADLE_JAVA_TOOL_OPTIONS)
                        configuration.settings.env = newEnv
                    } else if (configuration.settings.env.containsKey(JAVA_TOOL_OPTIONS)) {
                        val newEnv = mutableMapOf<String, String>()
                        newEnv.putAll(configuration.settings.env)
                        newEnv.remove(JAVA_TOOL_OPTIONS)
                        configuration.settings.env = newEnv
                    }
                }
            })
        }
    }

    private fun reportUnknownTasksToPosthog(configuration: RunConfigurationBase<*>) {
        val buildSystem = extractBuildSystem(configuration)

        if (buildSystem != JavaBuildSystem.UNKNOWN) {
            val buildSystemName = buildSystem.name.lowercase()
            val taskNames = RunCfgTools.extractTasks(configuration).toMutableSet()
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