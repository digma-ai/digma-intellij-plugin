package org.digma.intellij.plugin.instrumentation

interface InstrumentationProvider {

    fun buildMethodObservabilityInfo(methodId: String): MethodObservabilityInfo
    fun addObservabilityDependency(methodId: String)
    fun addObservability(methodId: String)
}