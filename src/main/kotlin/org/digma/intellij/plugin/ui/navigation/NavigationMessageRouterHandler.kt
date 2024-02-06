package org.digma.intellij.plugin.ui.navigation

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.jcef.BaseMessageRouterHandler

class NavigationMessageRouterHandler(project: Project) : BaseMessageRouterHandler(project) {


    override fun getOriginForTroubleshootingEvent(): String {
        return "navigation"
    }

    override fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String, action: String) {

        Log.log(logger::trace, project, "got action '$action' with message $requestJsonNode")

        when (action) {

            "ASSETS/INITIALIZE" -> onInitialize(browser)

            else -> {
                Log.log(logger::warn, "got unexpected action='$action'")
            }
        }
    }


    private fun onInitialize(browser: CefBrowser) {
        try {
            doCommonInitialize(browser)
        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, e, "error getting backend info")
        }
    }


}
