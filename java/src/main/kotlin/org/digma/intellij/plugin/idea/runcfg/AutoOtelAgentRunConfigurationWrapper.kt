package org.digma.intellij.plugin.idea.runcfg

import com.intellij.execution.JavaTestConfigurationBase
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.ModuleRunConfiguration
import com.intellij.execution.configurations.ParametersList
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.common.buildEnvForLocalTests
import org.digma.intellij.plugin.idea.deps.ModulesDepsService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.settings.SettingsState
import org.digma.intellij.plugin.settings.SpringBootObservabilityMode
import org.jetbrains.annotations.NotNull
import org.jetbrains.idea.maven.execution.MavenRunConfiguration
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration

private const val ORG_GRADLE_JAVA_TOOL_OPTIONS = "ORG_GRADLE_JAVA_TOOL_OPTIONS"

class AutoOtelAgentRunConfigurationWrapper : IRunConfigurationWrapper {

    private val logger: Logger = Logger.getInstance(AutoOtelAgentRunConfigurationWrapper::class.java)

    companion object {
        const val DIGMA_OBSERVABILITY_ENV_VAR_NAME = "DIGMA_OBSERVABILITY"

        @JvmStatic
        fun getInstance(project: Project): AutoOtelAgentRunConfigurationWrapper {
            return project.getService(AutoOtelAgentRunConfigurationWrapper::class.java)
        }
    }

    override fun getRunConfigType(configuration: RunConfigurationBase<*>, module: Module?): RunConfigType {
        return evalRunConfigType(configuration)
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

        val project = configuration.project
        val isSpringBootWithMicrometerTracing = evalSpringBootMicrometerTracing(resolvedModule)
        val runConfigType = evalRunConfigType(configuration)

        when (runConfigType) {
            RunConfigType.JavaTest,
            RunConfigType.JavaRun, -> {
                //this also works for: CommonJavaRunConfigurationParameters
                //params.vmParametersList.addParametersString("-verbose:class -javaagent:/home/shalom/tmp/run-configuration/opentelemetry-javaagent.jar")
                //params.vmParametersList.addProperty("myprop","myvalue")
                val javaToolOptions =
                    buildJavaToolOptions(configuration, project, isSpringBootWithMicrometerTracing, isOtelServiceNameAlreadyDefined(params), runConfigType.isTest)
                javaToolOptions?.let {
                    mergeJavaToolOptions(params, it)
                }
            }

            RunConfigType.GradleTest,
            RunConfigType.GradleRun, -> {
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
                val javaToolOptions =
                    buildJavaToolOptions(configuration, project, isSpringBootWithMicrometerTracing, isOtelServiceNameAlreadyDefined(configuration), runConfigType.isTest)
                javaToolOptions?.let {
                    mergeGradleJavaToolOptions(configuration, javaToolOptions)
                }
            }

            RunConfigType.MavenTest,
            RunConfigType.MavenRun, -> {
                configuration as MavenRunConfiguration
                val javaToolOptions =
                    buildJavaToolOptions(configuration, project, isSpringBootWithMicrometerTracing, isOtelServiceNameAlreadyDefined(params), runConfigType.isTest)
                javaToolOptions?.let {
                    mergeJavaToolOptions(params, it)
                }
            }

            else -> {
                // do nothing
            }
        } // end when case
    }

    // this one is used AFTER of settings.observability enabled
    private fun evalSpringBootMicrometerTracing(module: Module?): Boolean {
        if (module == null) return false
        val useAgentForSpringBoot = getConfigUseAgentForSpringBoot()

        val modulesDepsService = ModulesDepsService.getInstance(module.project)
        val moduleExt = modulesDepsService.getModuleExt(module.name)
        if (moduleExt != null) {
            if (moduleExt.metadata.hasSpringBoot() && !useAgentForSpringBoot) {
                // use Micrometer tracing, instead of OtelAgent
                return true
            }
        }
        return false
    }

    //this is only for gradle. we need to keep original JAVA_TOOL_OPTIONS if exists and restore when the process is
    // finished, anyway we need to clean our JAVA_TOOL_OPTIONS because it will be saved in the run configuration settings.
    private fun mergeGradleJavaToolOptions(configuration: GradleRunConfiguration, myJavaToolOptions: String) {
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


    //this is for java and maven run configurations. merge in case users have their own JAVA_TOOL_OPTIONS
    private fun mergeJavaToolOptions(params: JavaParameters, myJavaToolOptions: String) {
        var javaToolOptions = myJavaToolOptions
        if (params.env.containsKey(JAVA_TOOL_OPTIONS)) {
            val currentJavaToolOptions = params.env[JAVA_TOOL_OPTIONS]
            javaToolOptions = "$myJavaToolOptions $currentJavaToolOptions"
        }
        params.env[JAVA_TOOL_OPTIONS] = javaToolOptions
    }

    private fun buildJavaToolOptions(
        configuration: RunConfigurationBase<*>,
        project: Project,
        isSpringBootWithMicrometerTracing: Boolean,
        serviceAlreadyDefined: Boolean,
        isTest: Boolean,
    ): String? {

        val otelAgentPath = OTELJarProvider.getInstance().getOtelAgentJarPath(project)
        val digmaExtensionPath = OTELJarProvider.getInstance().getDigmaAgentExtensionJarPath(project)
        if (otelAgentPath == null || digmaExtensionPath == null) {
            Log.log(
                logger::warn,
                "could not build $JAVA_TOOL_OPTIONS because otel agent or digma extension jar are not available. please check the logs"
            )
            return null
        }

        var retVal = " "
        if (isSpringBootWithMicrometerTracing) {
            retVal = retVal
                .plus("-Dmanagement.otlp.tracing.endpoint=${getExporterUrl()}")
                .plus(" ")
                .plus("-Dmanagement.tracing.sampling.probability=1.0")
                .plus(" ")
        } else {
            retVal = retVal
                .plus("-javaagent:$otelAgentPath")
                .plus(" ")
                .plus("-Dotel.javaagent.extensions=$digmaExtensionPath")
                .plus(" ")
                .plus("-Dotel.exporter.otlp.traces.endpoint=${getExporterUrl()}")
                .plus(" ")
        }

        if (isTest) {
            val envPart = "digma.environment=${buildEnvForLocalTests()}"
            retVal = retVal
                .plus("-Dotel.resource.attributes=\"$envPart\"")
                .plus(" ")
        }

        retVal = retVal
            .plus("-Dotel.traces.exporter=otlp")
            .plus(" ")
            .plus("-Dotel.metrics.exporter=none")
            .plus(" ")

        if (!serviceAlreadyDefined) {
            retVal = retVal
                .plus("-Dotel.service.name=${evalServiceName(configuration)}")
                .plus(" ")
        }

        return retVal
    }

    private fun isOtelEntryDefined(
        javaConfParams: JavaParameters,
        environmentVariable: String,
        systemProperty: String,
    ): Boolean {
        return javaConfParams.env.containsKey(environmentVariable)
                || isVmEntryExists(javaConfParams, systemProperty)
    }

    /**
     * see <a href="https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md#opentelemetry-resource"></a>
     */
    private fun isOtelServiceNameAlreadyDefined(javaConfParams: JavaParameters): Boolean {
        return isOtelEntryDefined(javaConfParams, "OTEL_SERVICE_NAME", "otel.service.name")
    }

    // when its Gradle config check the GradleRunConfiguration.settings, and not the JavaParameters, since JavaParameters do not contain the relevant data
    private fun isOtelServiceNameAlreadyDefined(config: GradleRunConfiguration): Boolean {
        val vmParList = ParametersList()
        vmParList.addParametersString(config.settings.vmOptions)

        return config.settings.env.containsKey("OTEL_SERVICE_NAME")
                || vmParList.hasProperty("otel.service.name")
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

    private fun getConfigUseAgentForSpringBoot(): Boolean {
        return SettingsState.getInstance().springBootObservabilityMode == SpringBootObservabilityMode.OtelAgent
    }

    @NotNull
    private fun evalRunConfigType(configuration: RunConfigurationBase<*>): RunConfigType {
        if (isJavaConfiguration(configuration)) return RunConfigType.JavaRun
        if (isJavaTestConfiguration(configuration)) return RunConfigType.JavaTest
        if (isGradleConfiguration(configuration)) return RunConfigType.GradleRun
        if (isGradleTestConfiguration(configuration)) return RunConfigType.GradleTest
        if (isMavenConfiguration(configuration)) return RunConfigType.MavenRun
        if (isMavenTestConfiguration(configuration)) return RunConfigType.MavenTest
        return RunConfigType.Unknown
    }

    private fun isJavaConfiguration(configuration: RunConfigurationBase<*>): Boolean {
        /*
        this will catch the following run configuration types:
        ApplicationConfiguration,JUnitConfiguration,TestNGConfiguration,KotlinRunConfiguration,
        KotlinStandaloneScriptRunConfiguration,GroovyScriptRunConfiguration,JarApplicationConfiguration
         */
        return configuration is ApplicationConfiguration
    }

    private fun isJavaTestConfiguration(configuration: RunConfigurationBase<*>): Boolean {
        /*
        this will catch the following run configuration types:
        JavaTestConfigurationBase, TestNGConfiguration
         */
        return configuration is JavaTestConfigurationBase
    }

    override fun isGradleConfiguration(configuration: RunConfigurationBase<*>): Boolean {
        /*
        this will catch gradle running a main method or spring bootRun.
        all other gradle tasks are ignored.
         */
        if (configuration is GradleRunConfiguration) {
            val taskNames = configuration.settings.taskNames
            val isMainMethod = taskNames.any {
                it.contains(".main")
            }
            val hasBootRun = taskNames.any {
                it.contains(":bootRun") || it.equals("bootRun")
            }
            if (isMainMethod || hasBootRun) {
                return true
            }

            val digmaObservabilityEnvVarValue = configuration.settings.env.get(DIGMA_OBSERVABILITY_ENV_VAR_NAME)
            if (digmaObservabilityEnvVarValue != null
                && ("true".equals(digmaObservabilityEnvVarValue.trim(), true)
                        || "yes".equals(digmaObservabilityEnvVarValue.trim(), true)
                        )
            ) {
                return true
            }

            // check for standard OpenTelemetry environment variables based on common prefix OTEL_
            val hasOtelDefined = configuration.settings.env.keys.any {
                it.startsWith("OTEL_")
            }

            if (hasOtelDefined) {
                return true
            }
        }

        return false
    }

    private fun isGradleTestConfiguration(configuration: RunConfigurationBase<*>): Boolean {
        /*
        this will catch gradle running a main method or a unit test or spring bootRun.
        all other gradle tasks are ignored.
         */
        if (configuration is GradleRunConfiguration) {
            val taskNames = configuration.settings.taskNames
            val hasTestTask = taskNames.any {
                it.contains(":test") || it.equals("test")
            }
            return hasTestTask
        }
        return false
    }

    private fun isMavenConfiguration(configuration: RunConfigurationBase<*>): Boolean {
        //will catch maven exec plugin goals, bootRun
        if (configuration is MavenRunConfiguration) {
            val goalNames = configuration.runnerParameters.goals
            val hasRelevantTask = goalNames.any {
                false
                        || it.equals("exec:exec")
                        || it.equals("exec:java")
                        || it.equals("-Dexec.executable=java")
                        || it.equals("spring-boot:run")
                        || (it.contains(":spring-boot-maven-plugin:") && it.endsWith(":run"))
            }
            return hasRelevantTask
        }
        return false
    }

    private fun isMavenTestConfiguration(configuration: RunConfigurationBase<*>): Boolean {
        //will unit tests
        if (configuration is MavenRunConfiguration) {
            val goalNames = configuration.runnerParameters.goals
            val hasTestTask = goalNames.any {
                false
                        || it.equals("surefire:test")
                        || (it.contains(":maven-surefire-plugin:") && it.endsWith(":test"))
                        || it.equals("spring-boot:test-run")
                        || (it.contains(":spring-boot-maven-plugin:") && it.endsWith(":test-run"))
            }
            return hasTestTask
        }
        return false
    }

}