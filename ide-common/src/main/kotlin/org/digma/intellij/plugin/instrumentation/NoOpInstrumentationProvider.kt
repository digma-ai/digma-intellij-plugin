package org.digma.intellij.plugin.instrumentation

class NoOpInstrumentationProvider : InstrumentationProvider {
    override fun buildMethodObservabilityInfo(methodId: String): MethodObservabilityInfo {
        return MethodObservabilityInfo(methodId, hasMissingDependency = false, canInstrumentMethod = false)
    }

    override fun addObservabilityDependency(methodId: String) {
    }

    override fun addObservability(methodId: String) {
    }
}