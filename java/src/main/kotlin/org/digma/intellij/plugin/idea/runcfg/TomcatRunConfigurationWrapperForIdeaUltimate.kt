package org.digma.intellij.plugin.idea.runcfg

import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.ModuleRunConfiguration
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.settings.SettingsState

@Service(Service.Level.PROJECT)
class TomcatRunConfigurationWrapperForIdeaUltimate: RunConfigurationWrapper {


    private val logger: Logger = Logger.getInstance(this::class.java)

    companion object {
        @JvmStatic
        fun getInstance(project: Project): TomcatRunConfigurationWrapperForIdeaUltimate {
            return project.service<TomcatRunConfigurationWrapperForIdeaUltimate>()
        }
    }


    override fun getRunConfigType(configuration: RunConfiguration, module: Module?): RunConfigType {
        if (configuration.type.javaClass.simpleName == "TomcatConfiguration"){
            return RunConfigType.TomcatForIdeaUltimate
        }
        return RunConfigType.Unknown
    }

    override fun <T : RunConfigurationBase<*>?> updateJavaParameters(
        configuration: T & Any,
        params: JavaParameters,
        runnerSettings: RunnerSettings?,
        resolvedModule: Module?,
    ) {

        val javaToolOptions =
            buildJavaToolOptions(configuration, isOtelServiceNameAlreadyDefined(params))
        javaToolOptions?.let {
            OtelRunConfigurationExtension.mergeJavaToolOptions(params, it)
        }

    }



    private fun buildJavaToolOptions(
        configuration: RunConfigurationBase<*>,
        serviceAlreadyDefined: Boolean,
    ): String? {

        val otelAgentPath = service<OTELJarProvider>().getOtelAgentJarPath()
        val digmaExtensionPath = service<OTELJarProvider>().getDigmaAgentExtensionJarPath()
        if (otelAgentPath == null || digmaExtensionPath == null) {
            Log.log(
                logger::warn,
                "could not build $JAVA_TOOL_OPTIONS because otel agent or digma extension jar are not available. please check the logs"
            )
            return null
        }

        var retVal = " "
        retVal = retVal
            .plus("-javaagent:$otelAgentPath")
            .plus(" ")
            .plus("-Dotel.javaagent.extensions=$digmaExtensionPath")
            .plus(" ")
            .plus("-Dotel.exporter.otlp.traces.endpoint=${getExporterUrl()}")
            .plus(" ")
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



    private fun getExporterUrl(): String {
        return SettingsState.getInstance().runtimeObservabilityBackendUrl
    }

    private fun evalServiceName(configuration: RunConfigurationBase<*>): String {
        return if (configuration is ModuleRunConfiguration && configuration.modules.isNotEmpty()) {
            val moduleName = configuration.modules.first().name
            moduleName.replace(" ", "").trim()
        } else {
            configuration.project.name.replace(" ", "").trim()
        }
    }



    //not easy to discover if the settings contains -Dotel.service.name or OTEL_SERVICE_NAME.
    //there is a field named mySettingsBean that contains this info and can maybe be accessed with reflection.
    //but I noticed that if adding -Dotel.service.name=myservice to vm arguments it works.
    //or adding an env variable JAVA_OPTS=-Dotel.service.name=myservice
    //
    private fun isOtelServiceNameAlreadyDefined(params: JavaParameters): Boolean {
        return params.vmParametersList.hasProperty("otel.service.name")
    }

    override fun isGradleConfiguration(configuration: RunConfiguration): Boolean {
        return false
    }
}