package org.digma.intellij.plugin.instrumentation

class NoOpInstrumentationProvider : InstrumentationProvider {
    override suspend fun buildMethodObservabilityInfo(methodId: String): MethodObservabilityInfo =
        MethodObservabilityInfo(methodId, hasMissingDependency = false, canInstrumentMethod = false, hasAnnotation = false)

    override suspend fun addObservabilityDependency(methodId: String) = Unit
    override suspend fun addObservability(methodId: String) = Unit

    override suspend fun instrumentMethod(methodObservabilityInfo: MethodObservabilityInfo): Boolean = false
}