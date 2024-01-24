package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.Disposable

interface TestsService : Disposable {
    fun getScopeRequest(): ScopeRequest
    fun getLatestTestsOfSpan(scopeRequest: ScopeRequest, filter: FilterForLatestTests): String
    fun refresh()
    fun setPageSize(pageSize: Int)
}

data class ScopeRequest(
    val spanCodeObjectIds: Set<String>,
    val methodCodeObjectId: String?,
    val endpointCodeObjectId: String?

){
    fun isEmpty(): Boolean{
        return spanCodeObjectIds.isEmpty() && methodCodeObjectId.isNullOrBlank() && endpointCodeObjectId.isNullOrBlank()
    }
}

data class FilterForLatestTests(
    var environments: Set<String>,
    var pageNumber: Int = 1,
)
