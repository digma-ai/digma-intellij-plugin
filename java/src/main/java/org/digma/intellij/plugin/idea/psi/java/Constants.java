package org.digma.intellij.plugin.idea.psi.java;

public interface Constants {

    String OPENTELEMETY_FQN = "io.opentelemetry.api.OpenTelemetry";
    String GLOBAL_OPENTELEMETY_FQN = "io.opentelemetry.api.GlobalOpenTelemetry";
    String TRACER_BUILDER_FQN = "io.opentelemetry.api.trace.TracerBuilder";
    String WITH_SPAN_FQN = "io.opentelemetry.instrumentation.annotations.WithSpan";
    String WITH_SPAN_DEPENDENCY_DESCRIPTION = "opentelemetry.annotation"  ;
    String WITH_SPAN_INST_LIBRARY_1 = "io.opentelemetry.spring-boot-autoconfigure";
    String WITH_SPAN_INST_LIBRARY_2 = "io.opentelemetry.opentelemetry-instrumentation-annotations-1.16";
    String WITH_SPAN_INST_LIBRARY_3 = "io.quarkus.opentelemetry";
    String WITH_SPAN_INST_LIBRARY_4 = "io.micronaut.code";
    String SPAN_BUILDER_FQN = "io.opentelemetry.api.trace.SpanBuilder";
    String TRACER_FQN = "io.opentelemetry.api.trace.Tracer";
}
