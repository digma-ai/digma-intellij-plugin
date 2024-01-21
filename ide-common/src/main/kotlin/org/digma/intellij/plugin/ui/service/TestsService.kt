package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.Disposable
import org.cef.browser.CefBrowser

interface TestsService : Disposable {
    fun initWith(cefBrowser: CefBrowser, fillerOfLatestTests: FillerOfLatestTests, pageSize: Int)
    fun getScopeRequest(): ScopeRequest?

    fun getLatestTestsOfSpan(scopeRequest: ScopeRequest, filter: FilterForLatestTests): String

    // refresh the view of current tests view
    fun refresh()

    // handles request of GLOBAL/SET_ENVIRONMENTS
    fun sendOperativeEnvironments()
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

interface FillerOfLatestTests {

    fun fillDataOfTests(cefBrowser: CefBrowser, scopeRequest: ScopeRequest)
}

data class FilterForLatestTests(
    var environments: Set<String>,
    var pageNumber: Int = 1,
)
