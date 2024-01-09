package org.digma.intellij.plugin.ui.tests.model

data class ScopeRequest(
    val spanCodeObjectIds: Set<String>,
    val methodCodeObjectId: String?,
    val endpointCodeObjectId: String?,
)
