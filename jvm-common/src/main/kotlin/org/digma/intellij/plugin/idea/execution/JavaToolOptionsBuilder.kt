package org.digma.intellij.plugin.idea.execution

import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.SimpleProgramParameters
import org.digma.intellij.plugin.env.Env
import org.digma.intellij.plugin.settings.SettingsState

open class JavaToolOptionsBuilder(
    protected val configuration: RunConfigurationBase<*>,
    protected val params: SimpleProgramParameters,
    protected val parametersExtractor: ParametersExtractor,
    protected val serviceNameProvider: ServiceNameProvider = ServiceNameProvider(configuration, params)
) {

    private val otelAgentPathProvider = OtelAgentPathProvider(configuration)

    protected val javaToolOptions = StringBuilder(" ")

    open fun withOtelSdkDisabled(): JavaToolOptionsBuilder {
        javaToolOptions
            .append("-Dotel.sdk.disabled=false")
            .append(" ")
        return this
    }

    open fun withOtelExportedEndpoint(): JavaToolOptionsBuilder {
        javaToolOptions
            .append("-Dotel.exporter.otlp.endpoint=${getExporterUrl()}")
            .append(" ")
        return this
    }

    open fun withOtelAgent(useAgent: Boolean): JavaToolOptionsBuilder {

        if (useAgent) {
            if (!otelAgentPathProvider.hasAgentPath()) {
                throw JavaToolOptionsBuilderException("useAgent is true but can't find agent paths")
            }
            javaToolOptions
                .append("-javaagent:${otelAgentPathProvider.otelAgentPath}")
                .append(" ")
                .append("-Dotel.javaagent.extensions=${otelAgentPathProvider.digmaExtensionPath}")
                .append(" ")
                .append("-Dotel.exporter.otlp.traces.endpoint=${getExporterUrl()}")
                .append(" ")
        }

        return this
    }


    open fun withSpringBootWithMicrometerTracing(isSpringBootWithMicrometerTracing: Boolean): JavaToolOptionsBuilder {
        if (isSpringBootWithMicrometerTracing) {
            javaToolOptions
                .append("-Dmanagement.otlp.tracing.endpoint=${getExporterUrl()}")
                .append(" ")
                .append("-Dmanagement.tracing.sampling.probability=1.0")
                .append(" ")
        }

        return this
    }

    open fun withMicronautTracing(isMicronautModule: Boolean): JavaToolOptionsBuilder {
        if (isMicronautModule) {
            javaToolOptions
                .append("-Dotel.java.global-autoconfigure.enabled=true")
                .append(" ")
                .append("-Dotel.traces.exporter=otlp")
                .append(" ")
                .append("-Dotel.exporter.otlp.endpoint=${getExporterUrl()}")
                .append(" ")
                .append("-Dotel.exporter.otlp.insecure=true")
                .append(" ")
                .append("-Dotel.exporter.otlp.compression=gzip")
                .append(" ")
                .append("-Dotel.exporter.experimental.expoter.otlp.retry.enabled=true")
                .append(" ")
        }

        return this
    }

    open fun withTest(isTest: Boolean): JavaToolOptionsBuilder {
        if (isTest) {
            if (!parametersExtractor.alreadyHasTestEnv(configuration, params)) {
                val envPart = "digma.environment=${Env.buildEnvForLocalTests()}"
                javaToolOptions
                    .append("-Dotel.resource.attributes=\"$envPart\"")
                    .append(" ")
            }

            val hasMockito = true //currently do not check for mockito since flag is minor and won't affect other cases
            if (hasMockito) {
                // based on git issue https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/8862#issuecomment-1619722050 it seems to help
                javaToolOptions
                    .append("-Dotel.javaagent.experimental.field-injection.enabled=false")
                    .append(" ")
            }
        }

        return this
    }


    open fun withOtelDebug(): JavaToolOptionsBuilder {
        if (java.lang.Boolean.getBoolean("digma.otel.debug")) {
            javaToolOptions
                .append("-Dotel.javaagent.debug=true")
                .append(" ")
        }
        return this
    }


    open fun withExtendedObservability(): JavaToolOptionsBuilder {
        if (!SettingsState.getInstance().extendedObservability.isNullOrBlank()) {
            javaToolOptions
                .append("-Ddigma.autoinstrument.packages=${SettingsState.getInstance().extendedObservability}")
                .append(" ")
        }
        return this
    }

    open fun withCommonProperties(): JavaToolOptionsBuilder {
        javaToolOptions
            .append(getCommonOtelSystemProperties())
            .append(" ")
        return this
    }


    open fun withServiceName(moduleResolver: ModuleResolver): JavaToolOptionsBuilder {
        if (!parametersExtractor.isOtelServiceNameAlreadyDefined()) {
            javaToolOptions
                .append("-Dotel.service.name=${serviceNameProvider.provideServiceName(moduleResolver)}")
                .append(" ")
        }
        return this
    }


    open fun build(): String {
        return javaToolOptions.toString()
    }


    open fun getExporterUrl(): String {
        return SettingsState.getInstance().runtimeObservabilityBackendUrl
    }


    open fun getCommonOtelSystemProperties(): String {

        return listOf(
            "-Dotel.traces.exporter=otlp",
            "-Dotel.exporter.otlp.protocol=grpc",
            "-Dotel.metrics.exporter=none",
            "-Dotel.logs.exporter=none",
            "-Dotel.instrumentation.common.experimental.controller.telemetry.enabled=true",
            "-Dotel.instrumentation.common.experimental.view.telemetry.enabled=true",
            "-Dotel.instrumentation.experimental.span-suppression-strategy=none"
        ).joinToString(separator = " ")

    }


}


class JavaToolOptionsBuilderException(message: String) : RuntimeException(message)