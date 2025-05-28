package org.digma.intellij.plugin.instrumentation

interface InstrumentationProvider {
    suspend fun buildMethodObservabilityInfo(methodId: String): MethodObservabilityInfo
    suspend fun addObservabilityDependency(methodId: String)
    suspend fun addObservability(methodId: String)
    suspend fun instrumentMethod(methodObservabilityInfo: MethodObservabilityInfo): Boolean
}