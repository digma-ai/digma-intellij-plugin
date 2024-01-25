package org.digma.intellij.plugin.model.rest.tests

data class TestsScopeRequest(
    val spanCodeObjectIds: Set<String>,
    val methodCodeObjectId: String?,
    val endpointCodeObjectId: String?,
) {
    fun isEmpty(): Boolean {
        return spanCodeObjectIds.isEmpty() && methodCodeObjectId.isNullOrBlank() && endpointCodeObjectId.isNullOrBlank()
    }
}
