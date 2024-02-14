package org.digma.intellij.plugin.idea.runcfg

fun getOtelSystemProperties() : String{

    return listOf("-Dotel.traces.exporter=otlp",
          "-Dotel.exporter.otlp.protocol=grpc",
           "-Dotel.metrics.exporter=none",
           "-Dotel.logs.exporter=none",
           "-Dotel.instrumentation.common.experimental.controller.telemetry.enabled=true",
           "-Dotel.instrumentation.common.experimental.view.telemetry.enabled=true",
           "-Dotel.instrumentation.experimental.span-suppression-strategy=none")
        .joinToString(separator = " ")

}