package org.digma.intellij.plugin.ui.dashboard

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.dashboard.LegacyJavaDashboardMessageRouterHandler
import org.digma.intellij.plugin.ui.jcef.BaseMessageRouterHandler

class DashboardMessageRouterHandler(project: Project) : BaseMessageRouterHandler(project) {

    private val legacyJava = LegacyJavaDashboardMessageRouterHandler(project, this)

    override fun getOriginForTroubleshootingEvent(): String {
        return "dashboard"
    }

    override suspend fun doOnQuery(
        project: Project,
        browser: CefBrowser,
        requestJsonNode: JsonNode,
        rawRequest: String,
        action: String
    ): Boolean {
        return legacyJava.doOnQueryJavaLegacy(browser, requestJsonNode, action)
    }
}