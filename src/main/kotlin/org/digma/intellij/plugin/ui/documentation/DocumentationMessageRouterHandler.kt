package org.digma.intellij.plugin.ui.documentation

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.ui.jcef.BaseMessageRouterHandler

class DocumentationMessageRouterHandler(project: Project) : BaseMessageRouterHandler(project) {

    override fun getOriginForTroubleshootingEvent(): String {
        return "documentation"
    }

    @Throws(Exception::class)
    override suspend fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String, action: String): Boolean {
        when (action) {
            "DOCUMENTATION/INITIALIZE" -> doCommonInitialize(browser)
            else -> {
                return false
            }
        }
        return true
    }
}