package org.digma.intellij.plugin.idea.runcfg

import com.intellij.execution.JavaTestConfigurationBase
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.ModuleRunConfiguration
import com.intellij.execution.configurations.ParametersList
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.common.FileUtils
import org.digma.intellij.plugin.common.StringUtils.Companion.evalBoolean
import org.digma.intellij.plugin.env.Env
import org.digma.intellij.plugin.idea.deps.ModuleMetadata
import org.digma.intellij.plugin.idea.deps.ModulesDepsService
import org.digma.intellij.plugin.idea.psi.kotlin.isKotlinRunConfiguration
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.settings.SettingsState
import org.digma.intellij.plugin.settings.SpringBootObservabilityMode
import org.jetbrains.annotations.NotNull
import org.jetbrains.idea.maven.execution.MavenRunConfiguration
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration

class AutoOtelAgentRunConfigurationWrapper : RunConfigurationWrapper {

    private val logger: Logger = Logger.getInstance(AutoOtelAgentRunConfigurationWrapper::class.java)

    companion object {
        private const val DIGMA_OBSERVABILITY_ENV_VAR_NAME = "DIGMA_OBSERVABILITY"

        @JvmStatic
        fun getInstance(project: Project): AutoOtelAgentRunConfigurationWrapper {
            return project.getService(AutoOtelAgentRunConfigurationWrapper::class.java)
        }
    }

    override fun getRunConfigType(configuration: RunConfiguration, module: Module?): RunConfigType {
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
        val isMicronautModule = evalIsMicronautModule(resolvedModule)
        val useAgent = !(isMicronautModule || isSpringBootWithMicrometerTracing)
        when (val runConfigType = evalRunConfigType(configuration)) {
            RunConfigType.JavaTest,
            RunConfigType.KotlinRun,
            RunConfigType.JavaRun,
            -> {
                //this also works for: CommonJavaRunConfigurationParameters
                //params.vmParametersList.addParametersString("-verbose:class -javaagent:/home/shalom/tmp/run-configuration/opentelemetry-javaagent.jar")
                //params.vmParametersList.addProperty("myprop","myvalue")
                val javaToolOptions =
                    buildJavaToolOptions(
                        configuration,
                        useAgent,
                        isSpringBootWithMicrometerTracing,
                        isOtelServiceNameAlreadyDefined(params),
                        runConfigType.isTest
                    )
                javaToolOptions?.let {
                    mergeJavaToolOptions(project, params, it)
                }
            }

            RunConfigType.GradleTest,
            RunConfigType.GradleRun,
            -> {
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
                    buildJavaToolOptions(
                        configuration,
                        useAgent,
                        isSpringBootWithMicrometerTracing,
                        isOtelServiceNameAlreadyDefined(configuration),
                        runConfigType.isTest
                    )
                javaToolOptions?.let {
                    mergeGradleJavaToolOptions(configuration, javaToolOptions)
                }
            }

            RunConfigType.MavenTest,
            RunConfigType.MavenRun,
            -> {
                configuration as MavenRunConfiguration
                val javaToolOptions =
                    buildJavaToolOptions(
                        configuration,
                        useAgent,
                        isSpringBootWithMicrometerTracing,
                        isOtelServiceNameAlreadyDefined(params),
                        runConfigType.isTest
                    )
                javaToolOptions?.let {
                    mergeJavaToolOptions(project, params, it)
                }
            }

            else -> {
                // do nothing
            }
        } // end when case
    }

    // this one is used AFTER of settings.observability enabled
    private fun evalSpringBootMicrometerTracing(module: Module?): Boolean {
        val useAgentForSpringBoot = getConfigUseAgentForSpringBoot()

        if (module == null)
            return !useAgentForSpringBoot

        val moduleMetadata = getModuleMetadata(module)
        if (moduleMetadata != null) {
            if (moduleMetadata.hasSpringBoot() && !useAgentForSpringBoot) {
                // use Micrometer tracing, instead of OtelAgent
                return true
            }
        }
        return false
    }

    private fun evalIsMicronautModule(module: Module?): Boolean {
        val moduleMetadata = getModuleMetadata(module)
        return moduleMetadata?.hasMicronaut() ?: false
    }

    private fun getModuleMetadata(module: Module?): ModuleMetadata? {
        if (module == null)
            return null

        val modulesDepsService = ModulesDepsService.getInstance(module.project)
        val moduleExt = modulesDepsService.getModuleExt(module.name)
        if (moduleExt == null) {
            return null
        }
        return moduleExt.metadata
    }

    private fun buildJavaToolOptions(
        configuration: RunConfigurationBase<*>,
        useAgent: Boolean,
        isSpringBootWithMicrometerTracing: Boolean,
        serviceAlreadyDefined: Boolean,
        isTest: Boolean,
    ): String? {
        var otelAgentPath = service<OTELJarProvider>().getOtelAgentJarPath()
        var digmaExtensionPath = service<OTELJarProvider>().getDigmaAgentExtensionJarPath()
        if (otelAgentPath == null || digmaExtensionPath == null) {
            Log.log(
                logger::warn,
                "could not build $JAVA_TOOL_OPTIONS because otel agent or digma extension jar are not available. please check the logs"
            )
            return null
        }

        if (isWsl(configuration)) {
            otelAgentPath = FileUtils.convertWinToWslPath(otelAgentPath)
            digmaExtensionPath = FileUtils.convertWinToWslPath(digmaExtensionPath)
        }

        var retVal = " -noverify "
        if (isSpringBootWithMicrometerTracing) {
            retVal = retVal
                .plus("-Dmanagement.otlp.tracing.endpoint=${getExporterUrl()}")
                .plus(" ")
                .plus("-Dmanagement.tracing.sampling.probability=1.0")
                .plus(" ")
        }

        if (useAgent) {
            retVal = retVal
                .plus("-javaagent:$otelAgentPath")
                .plus(" ")
                .plus("-Dotel.javaagent.extensions=$digmaExtensionPath")
                .plus(" ")
                .plus("-Dotel.exporter.otlp.traces.endpoint=${getExporterUrl()}")
                .plus(" ")
        }

        if (isTest) {
            val envPart = "digma.environment=${Env.buildEnvForLocalTests()}"
            retVal = retVal
                .plus("-Dotel.resource.attributes=\"$envPart\"")
                .plus(" ")

            val hasMockito = true //currently do not check for mockito since flag is minor and won't affect other cases
            if (hasMockito) {
                // based on git issue https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/8862#issuecomment-1619722050 it seems to help
                retVal = retVal
                    .plus("-Dotel.javaagent.experimental.field-injection.enabled=false")
                    .plus(" ")
            }
        }

        retVal = retVal
            .plus(getOtelSystemProperties())
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
    private fun evalRunConfigType(configuration: RunConfiguration): RunConfigType {
        if (isJavaConfiguration(configuration)) return RunConfigType.JavaRun
        if (isKotlinConfiguration(configuration)) return RunConfigType.KotlinRun
        if (isJavaTestConfiguration(configuration)) return RunConfigType.JavaTest
        if (isGradleConfiguration(configuration)) return RunConfigType.GradleRun
        if (isGradleTestConfiguration(configuration)) return RunConfigType.GradleTest
        if (isMavenConfiguration(configuration)) return RunConfigType.MavenRun
        if (isMavenTestConfiguration(configuration)) return RunConfigType.MavenTest
        return RunConfigType.Unknown
    }

    private fun isKotlinConfiguration(configuration: RunConfiguration): Boolean {
        return isKotlinRunConfiguration(configuration)
    }

    private fun isJavaConfiguration(configuration: RunConfiguration): Boolean {
        /*
        this will catch the following run configuration types:
        ApplicationConfiguration,JUnitConfiguration,TestNGConfiguration,KotlinRunConfiguration,
        KotlinStandaloneScriptRunConfiguration,GroovyScriptRunConfiguration,JarApplicationConfiguration
         */
        return configuration is ApplicationConfiguration
    }

    private fun isJavaTestConfiguration(configuration: RunConfiguration): Boolean {
        /*
        this will catch the following run configuration types:
        JavaTestConfigurationBase, TestNGConfiguration
         */
        return configuration is JavaTestConfigurationBase
    }

    override fun isGradleConfiguration(configuration: RunConfiguration): Boolean {
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

            //support for the run task of the java application plugin. https://docs.gradle.org/current/userguide/application_plugin.html
            val hasRun = taskNames.any {
                it.contains(":run") || it.equals("run")
            }

            if (isMainMethod || hasBootRun || hasRun) {
                return true
            }

            val digmaObservabilityEnvVarValue = configuration.settings.env[DIGMA_OBSERVABILITY_ENV_VAR_NAME]
            if (digmaObservabilityEnvVarValue != null && evalBoolean(digmaObservabilityEnvVarValue)) {
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

    private fun isGradleTestConfiguration(configuration: RunConfiguration): Boolean {
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

    private fun isMavenConfiguration(configuration: RunConfiguration): Boolean {
        //will catch maven exec plugin goals, bootRun
        if (configuration is MavenRunConfiguration) {
            val goalNames = configuration.runnerParameters.goals
            val hasRelevantGoal = goalNames.any {
                false
                        || it.equals("exec:exec")
                        || it.equals("exec:java")
                        || it.equals("-Dexec.executable=java")
                        // spring boot
                        || it.equals("spring-boot:run")
                        || (it.contains(":spring-boot-maven-plugin:") && it.endsWith(":run"))
                        // tomcat6 and tomcat7. support goal named "tomcat7:run"
                        || (it.startsWith("tomcat") && it.endsWith(":run"))
                        //  support org.apache.tomcat.maven:tomcat7-maven-plugin:2.2:run
                        //   and    org.apache.tomcat.maven:tomcat6-maven-plugin:2.2:run
                        || (it.contains(":tomcat7-maven-plugin:") && it.endsWith(":run"))
                        || (it.contains(":tomcat6-maven-plugin:") && it.endsWith(":run"))
                        // support jetty, goal named "jetty:run"
                        || (it.startsWith("jetty") && it.endsWith(":run"))
                        // support jetty run via maven plugin ("org.eclipse.jetty:jetty-maven-plugin:10.0.18:run")
                        //  and     ("org.mortbay.jetty:jetty-maven-plugin:8.1.16.v20140903:run")
                        || (it.contains(":jetty-maven-plugin:") && it.endsWith(":run"))
            }
            if (hasRelevantGoal) return true

            // runnerSettings are not null when actually running the goal
            if (configuration.runnerSettings != null) {
                val envVarValue = configuration.runnerSettings!!.environmentProperties.get(DIGMA_OBSERVABILITY_ENV_VAR_NAME)
                if (envVarValue != null && evalBoolean(envVarValue)) {
                    return true
                }
            }
        }
        return false
    }

    private fun isMavenTestConfiguration(configuration: RunConfiguration): Boolean {
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