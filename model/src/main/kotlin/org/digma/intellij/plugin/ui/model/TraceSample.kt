package org.digma.intellij.plugin.ui.model

data class TraceSample(val traceName: String, val traceId: String?) {

    fun hasTraceId(): Boolean {
        return !traceId.isNullOrBlank()
    }
}