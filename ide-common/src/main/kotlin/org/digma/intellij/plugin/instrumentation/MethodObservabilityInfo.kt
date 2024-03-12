package org.digma.intellij.plugin.instrumentation

class MethodObservabilityInfo(
    val methodId: String,
    val hasMissingDependency: Boolean,
    val canInstrumentMethod: Boolean,
    val annotationClassFqn: String? = null,
    val hasAnnotation: Boolean,
)