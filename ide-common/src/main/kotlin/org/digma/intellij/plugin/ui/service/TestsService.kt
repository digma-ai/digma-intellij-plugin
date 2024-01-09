package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.Disposable

interface TestsService : Disposable {

    fun getLatestTestsOfSpan(scopeRequest: ScopeRequest, environments: Set<String>, pageNumber: Int, pageSize: Int): String

}

data class ScopeRequest(
    val spanCodeObjectIds: Set<String>,
    val methodCodeObjectId: String?,
    val endpointCodeObjectId: String?,
)
