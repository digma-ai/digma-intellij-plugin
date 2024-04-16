package org.digma.intellij.plugin.idea.execution

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.SimpleProgramParameters
import org.digma.intellij.plugin.analytics.LOCAL_ENV
import org.digma.intellij.plugin.analytics.LOCAL_TESTS_ENV
import org.digma.intellij.plugin.analytics.isCentralized
import org.digma.intellij.plugin.auth.account.DigmaDefaultAccountHolder
import org.digma.intellij.plugin.settings.SettingsState

open class JavaToolOptionsBuilder(
    protected val configuration: RunConfiguration,
    protected val params: SimpleProgramParameters,
    @Suppress("UNUSED_PARAMETER") runnerSettings: RunnerSettings?
) {

    private val otelAgentPathProvider = OtelAgentPathProvider(configuration)

    protected val javaToolOptions = StringBuilder(" ")

    private val otelResourceAttributes = mutableListOf<String>()


    open fun withOtelSdkDisabledEqualsFalse(): JavaToolOptionsBuilder {
        javaToolOptions
            .append("-Dotel.sdk.disabled=false")
            .append(" ")
        return this
    }

    open fun withOtelExporterEndpoint(): JavaToolOptionsBuilder {
        javaToolOptions
            .append("-Dotel.exporter.otlp.endpoint=${getExporterUrl()}")
            .append(" ")
        return this
    }

    open fun withOtelTracesExporterEndpoint(): JavaToolOptionsBuilder {
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

            withOtelTracesExporterEndpoint()
        }

        return this
    }

    open fun withCommonResourceAttributes(isTest: Boolean, parametersExtractor: ParametersExtractor): JavaToolOptionsBuilder {

        if (needToAddDigmaEnvironmentAttribute(parametersExtractor)) {
            val envAttribute = if (isTest) {
                "$DIGMA_ENVIRONMENT_RESOURCE_ATTRIBUTE=$LOCAL_TESTS_ENV"
            } else {
                "$DIGMA_ENVIRONMENT_RESOURCE_ATTRIBUTE=$LOCAL_ENV"
            }

            withOtelResourceAttribute(envAttribute)
        }

        if (isCentralized(configuration.project)) {
            DigmaDefaultAccountHolder.getInstance().account?.userId?.let {
                val userIdAttribute = "$DIGMA_USER_ID_RESOURCE_ATTRIBUTE=${it}"
                withOtelResourceAttribute(userIdAttribute)
            }
        }

        return this
    }


    private fun needToAddDigmaEnvironmentAttribute(parametersExtractor: ParametersExtractor): Boolean {
        return !parametersExtractor.hasDigmaEnvironmentIdAttribute(configuration, params) &&
                !parametersExtractor.hasDigmaEnvironmentAttribute(configuration, params)
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

            withOtelExporterEndpoint()
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


    open fun withOtelResourceAttribute(attribute: String): JavaToolOptionsBuilder {
        //collecting otel resource attributes , they are built in the build method
        otelResourceAttributes.add(attribute)
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

        if (otelResourceAttributes.isNotEmpty()) {
            val resourceAttributes = otelResourceAttributes.joinToString(",")
            javaToolOptions
                .append("-Dotel.resource.attributes=\"$resourceAttributes\"")
                .append(" ")
        }

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