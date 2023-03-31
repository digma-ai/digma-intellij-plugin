package org.digma.intellij.plugin.idea.runcfg

import com.intellij.execution.CommonJavaRunConfigurationParameters
import com.intellij.execution.Executor
import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.ModuleRunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.settings.SettingsState
import org.jetbrains.idea.maven.execution.MavenRunConfiguration
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration

private const val ORG_GRADLE_JAVA_TOOL_OPTIONS="ORG_GRADLE_JAVA_TOOL_OPTIONS"

class AutoOtelAgentRunConfigurationExtension : RunConfigurationExtension() {

    private val logger: Logger = Logger.getInstance(AutoOtelAgentRunConfigurationExtension::class.java)

    private fun enabled(project: Project): Boolean{
        return PersistenceService.getInstance(project).state.isAutoOtel
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
        Log.log(logger::debug,"isApplicableFor, project:{}, id:{}, name:{}, type:{}",
            configuration.project,configuration.id,configuration.name,configuration.type)

        if (isJavaConfiguration(configuration) || isGradleConfiguration(configuration) || isMavenConfiguration(configuration)){
            return true
        }

        return false
    }




    override fun <T : RunConfigurationBase<*>?> updateJavaParameters(
        configuration: T & Any,
        params: JavaParameters,
        runnerSettings: RunnerSettings?
    ) {

        Log.log(logger::debug,"updateJavaParameters, project:{}, id:{}, name:{}, type:{}",
            configuration.project,configuration.id,configuration.name,configuration.type)

        val project = configuration.project

        //testing if enabled must be done here just before running.
        if (!enabled(project)){
            Log.log(logger::debug,"AutoOtelAgentRunConfigurationExtension is not enabled")
            return
        }


        if (isJavaConfiguration(configuration)){
            //this also works for: CommonJavaRunConfigurationParameters
            //params.vmParametersList.addParametersString("-verbose:class -javaagent:/home/shalom/tmp/run-configuration/opentelemetry-javaagent.jar")
            //params.vmParametersList.addProperty("myprop","myvalue")
            val javaToolOptions = buildJavaToolOptions(configuration,project)
            javaToolOptions?.let {
                mergeJavaToolOptions(params,it)
            }

        }else if (isGradleConfiguration(configuration)){
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
            val javaToolOptions = buildJavaToolOptions(configuration,project)
            javaToolOptions?.let {
                mergeGradleJavaToolOptions(configuration,javaToolOptions)
            }
        }else if (isMavenConfiguration(configuration)){
            configuration as MavenRunConfiguration
            val javaToolOptions = buildJavaToolOptions(configuration,project)
            javaToolOptions?.let {
                mergeJavaToolOptions(params,it)
            }
        }
    }


    //this is only for gradle. we need to keep original JAVA_TOOL_OPTIONS if exists and restore when the process is
    // finished, anyway we need to clean our JAVA_TOOL_OPTIONS because it will be saved in the run configuration settings.
    private fun mergeGradleJavaToolOptions(configuration: GradleRunConfiguration, myJavaToolOptions: String) {
        var javaToolOptions = myJavaToolOptions
        //need to replace the env because it may be immutable map
        val newEnv = mutableMapOf<String, String>()
        if (configuration.settings.env.containsKey(JAVA_TOOL_OPTIONS)){
            val currentJavaToolOptions = configuration.settings.env[JAVA_TOOL_OPTIONS]
            javaToolOptions = "$myJavaToolOptions $currentJavaToolOptions"
            newEnv[ORG_GRADLE_JAVA_TOOL_OPTIONS] = currentJavaToolOptions!!
        }
        newEnv[JAVA_TOOL_OPTIONS] = javaToolOptions
        configuration.settings.env = newEnv
    }


    //this is for java and maven run configurations. merge in case users have their own JAVA_TOOL_OPTIONS
    private fun mergeJavaToolOptions(params: JavaParameters, myJavaToolOptions: String) {
        var javaToolOptions = myJavaToolOptions
        if (params.env.containsKey(JAVA_TOOL_OPTIONS)){
            val currentJavaToolOptions = params.env[JAVA_TOOL_OPTIONS]
            javaToolOptions = "$myJavaToolOptions $currentJavaToolOptions"
        }
        params.env[JAVA_TOOL_OPTIONS] = javaToolOptions
    }


    override fun attachToProcess(
        configuration: RunConfigurationBase<*>,
        handler: ProcessHandler,
        runnerSettings: RunnerSettings?
    ) {
        //we need to clean gradle configuration from our JAVA_TOOL_OPTIONS
        if (isGradleConfiguration(configuration)) {
            handler.addProcessListener(object : ProcessListener {

                override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {
                    cleanGradleSettings(configuration)
                }

                private fun cleanGradleSettings(configuration: RunConfigurationBase<*>) {
                    configuration as GradleRunConfiguration
                    Log.log(logger::debug,"Cleaning gradle configuration {}",configuration)
                    if (configuration.settings.env.containsKey(ORG_GRADLE_JAVA_TOOL_OPTIONS)){
                        val orgJavaToolOptions = configuration.settings.env[ORG_GRADLE_JAVA_TOOL_OPTIONS]
                        configuration.settings.env[JAVA_TOOL_OPTIONS] = orgJavaToolOptions
                        configuration.settings.env.remove(ORG_GRADLE_JAVA_TOOL_OPTIONS)
                    }else if (configuration.settings.env.containsKey(JAVA_TOOL_OPTIONS)){
                        configuration.settings.env.remove(JAVA_TOOL_OPTIONS)
                    }
                }
            })
        }

    }


    override fun decorate(
        console: ConsoleView,
        configuration: RunConfigurationBase<*>,
        executor: Executor
    ): ConsoleView {
        val project = configuration.project
        if (enabled(project)  &&
            (isMavenConfiguration(configuration) || isJavaConfiguration(configuration))){
            //that only works for java and maven run configurations.
            console.print("This process is enhanced by Digma OTEL agent !\n", ConsoleViewContentType.LOG_WARNING_OUTPUT)
        }

        return console
    }

    private fun buildJavaToolOptions(configuration: RunConfigurationBase<*>, project: Project): String? {

        val otelAgentPath = OTELJarProvider.getInstance().getOtelAgentJarPath(project)
        val digmaExtensionPath = OTELJarProvider.getInstance().getDigmaAgentExtensionJarPath(project)
        if (otelAgentPath == null || digmaExtensionPath == null){
            Log.log(logger::debug,"could not build $JAVA_TOOL_OPTIONS because otel agent or digma extension jar are not available. please check the logs")
            return null
        }

        return "".plus("-javaagent:$otelAgentPath")
            .plus(" ")
            .plus("-Dotel.javaagent.extensions=$digmaExtensionPath")
            .plus(" ")
            .plus("-Dotel.traces.exporter=otlp")
            .plus(" ")
            .plus("-Dotel.metrics.exporter=none")
            .plus(" ")
            .plus("-Dotel.exporter.otlp.traces.endpoint=${getExporterUrl(project)}")
            .plus(" ")
            .plus("-Dotel.service.name=${getServiceName(configuration)}")
            .plus(" ")
    }

    private fun getServiceName(configuration: RunConfigurationBase<*>): String {
        return if (configuration is ModuleRunConfiguration && configuration.modules.isNotEmpty()){
            val moduleName = configuration.modules.first().name
            moduleName.replace(" ","").trim()
        }else{
            configuration.project.name.replace(" ","").trim()
        }
    }

    private fun getExporterUrl(project: Project): String {
        return SettingsState.getInstance(project).runtimeObservabilityBackendUrl
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
        if (configuration is GradleRunConfiguration){
            val taskNames = configuration.settings.taskNames
            val isMainMethod = taskNames.any{
                it.contains(".main")
            }
            val hasTestTask = taskNames.any{
                it.contains(":test") || it.equals("test")
            }
            val hasBootRun = taskNames.any {
                it.contains(":bootRun") || it.equals("bootRun")
            }
            if (isMainMethod || hasTestTask || hasBootRun){
                return true
            }
        }

        return false
    }


    private fun isMavenConfiguration(configuration: RunConfigurationBase<*>): Boolean {
        //will catch maven exec plugin goals, bootRun and uni tests
        if (configuration is MavenRunConfiguration){
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