package org.digma.intellij.plugin.model.discovery

data class CodeLessSpan(
    val spanId: String,
    val spanInstLibrary: String,
    val spanName: String,
    val methodId: String?,
    val functionNamespace: String?,
    val functionName: String?
)
