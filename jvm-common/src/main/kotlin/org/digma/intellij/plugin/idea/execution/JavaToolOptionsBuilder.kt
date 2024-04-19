package org.digma.intellij.plugin.idea.execution

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.SimpleProgramParameters
import org.digma.intellij.plugin.analytics.LOCAL_TESTS_ENV
import org.digma.intellij.plugin.settings.SettingsState

open class JavaToolOptionsBuilder(
    protected val configuration: RunConfiguration,
    protected val params: SimpleProgramParameters,
    @Suppress("UNUSED_PARAMETER") runnerSettings: RunnerSettings?
) {

    private val otelAgentPathProvider = OtelAgentPathProvider(configuration)

    protected val javaToolOptions = StringBuilder(" ")


    open fun withOtelSdkDisabledEqualsFalse(): JavaToolOptionsBuilder {
        javaToolOptions
            .append("-Dotel.sdk.disabled=false")
            .append(" ")
        return this
    }

    open fun withOtelExporterOtlpEndpoint(): JavaToolOptionsBuilder {
        javaToolOptions
            .append("-Dotel.exporter.otlp.endpoint=${getExporterUrl()}")
            .append(" ")
        return this
    }

    open fun withOtelTracesExporterOtlpEndpoint(): JavaToolOptionsBuilder {
        javaToolOptions
            .append("-Dotel.exporter.otlp.traces.endpoint=${getExporterUrl()}")
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

            withOtelTracesExporterOtlpEndpoint()
        }

        return this
    }


    open fun withCommonSpringBootWithMicrometerTracing(isSpringBootWithMicrometerTracing: Boolean): JavaToolOptionsBuilder {
        if (isSpringBootWithMicrometerTracing) {
            withManagementOtlpTracesEndpoint()
            withManagementTracingSamplingProbability()
        }

        return this
    }

    //needed for spring boot with micrometer
    open fun withManagementOtlpTracesEndpoint(): JavaToolOptionsBuilder {
        javaToolOptions
            .append("-Dmanagement.otlp.tracing.endpoint=${getExporterUrl()}")
            .append(" ")
        return this
    }

    //needed for spring boot with micrometer
    open fun withManagementTracingSamplingProbability(): JavaToolOptionsBuilder {
        javaToolOptions
            .append("-Dmanagement.tracing.sampling.probability=1.0")
            .append(" ")
        return this
    }


    open fun withMicronautTracing(isMicronautTracing: Boolean): JavaToolOptionsBuilder {
        if (isMicronautTracing) {
            javaToolOptions
                .append("-Dotel.java.global-autoconfigure.enabled=true")
                .append(" ")
                .append("-Dotel.traces.exporter=otlp")
                .append(" ")
                .append("-Dotel.exporter.otlp.insecure=true")
                .append(" ")
                .append("-Dotel.exporter.otlp.compression=gzip")
                .append(" ")
                .append("-Dotel.exporter.experimental.exporter.otlp.retry.enabled=true")
                .append(" ")

            withOtelExporterOtlpEndpoint()
        }

        return this
    }

//    open fun withTest(isTest: Boolean, parametersExtractor: ParametersExtractor): JavaToolOptionsBuilder {
//        if (isTest && !isCentralized(configuration.project)) {
//            if (!parametersExtractor.hasDigmaEnvironmentIdAttribute(configuration, params)) {
//                val testEnv = "$DIGMA_ENVIRONMENT_RESOURCE_ATTRIBUTE=$LOCAL_TESTS_ENV"
//                withOtelResourceAttribute(testEnv)
//            }
//        }
//
//        return this
//    }


    open fun withMockitoSupport(isTest: Boolean): JavaToolOptionsBuilder {
        if (isTest) {
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

    //not every flavor needs that, the default flavor does,other flavors need only a subset
    open fun withCommonProperties(): JavaToolOptionsBuilder {

        withOtelTracesExporterOtlp()
        withOtelExporterOtlpProtocolGrpc()
        withOtelMetricsExporterNone()
        withOtelLogsExporterNone()
        withOtelExperimentalControllerTelemetryEnabled()
        withOtelExperimentalViewTelemetryEnabled()
        withOtelExperimentalSpanSuppressionStrategyNone()

        return this
    }


    open fun withOtelTracesExporterOtlp(): JavaToolOptionsBuilder {
        javaToolOptions
            .append("-Dotel.traces.exporter=otlp")
            .append(" ")
        return this
    }

    open fun withOtelExporterOtlpProtocolGrpc(): JavaToolOptionsBuilder {
        javaToolOptions
            .append("-Dotel.exporter.otlp.protocol=grpc")
            .append(" ")
        return this
    }

    open fun withOtelMetricsExporterNone(): JavaToolOptionsBuilder {
        javaToolOptions
            .append("-Dotel.metrics.exporter=none")
            .append(" ")
        return this
    }

    open fun withOtelLogsExporterNone(): JavaToolOptionsBuilder {
        javaToolOptions
            .append("-Dotel.logs.exporter=none")
            .append(" ")
        return this
    }

    open fun withOtelExperimentalControllerTelemetryEnabled(): JavaToolOptionsBuilder {
        javaToolOptions
            .append("-Dotel.instrumentation.common.experimental.controller.telemetry.enabled=true")
            .append(" ")
        return this
    }

    open fun withOtelExperimentalViewTelemetryEnabled(): JavaToolOptionsBuilder {
        javaToolOptions
            .append("-Dotel.instrumentation.common.experimental.view.telemetry.enabled=true")
            .append(" ")
        return this
    }

    open fun withOtelExperimentalSpanSuppressionStrategyNone(): JavaToolOptionsBuilder {
        javaToolOptions
            .append("-Dotel.instrumentation.experimental.span-suppression-strategy=none")
            .append(" ")
        return this
    }


    open fun withServiceName(
        moduleResolver: ModuleResolver,
        parametersExtractor: ParametersExtractor,
        serviceNameProvider: ServiceNameProvider
    ): JavaToolOptionsBuilder {
        if (!parametersExtractor.isOtelServiceNameAlreadyDefined()) {
            javaToolOptions
                .append("-Dotel.service.name=${serviceNameProvider.provideServiceName(moduleResolver)}")
                .append(" ")
        }
        return this
    }


    //only for quarkus
    fun withQuarkusTest(isTest: Boolean): JavaToolOptionsBuilder {
        if (isTest) {
            val envPart = "$DIGMA_ENVIRONMENT_RESOURCE_ATTRIBUTE=$LOCAL_TESTS_ENV"
            javaToolOptions
                .append("-Dquarkus.otel.resource.attributes=\"$envPart\"")
                .append(" ")
                .append("-Dquarkus.otel.bsp.schedule.delay=0.001S") // set delay to 1 millisecond
                .append(" ")
                .append("-Dquarkus.otel.bsp.max.export.batch.size=1") // by setting size of 1 it kind of disable
                .append(" ")
        }
        return this
    }

    fun withQuarkusOtelExportedTracesEndpoint(): JavaToolOptionsBuilder {
        javaToolOptions
            .append("-Dquarkus.otel.exporter.otlp.traces.endpoint=${getExporterUrl()}")
            .append(" ")

        return this
    }


    open fun build(): String {
        return javaToolOptions.toString()
    }


    open fun getExporterUrl(): String {
        return SettingsState.getInstance().runtimeObservabilityBackendUrl
    }

}


class JavaToolOptionsBuilderException(message: String) : RuntimeException(message)