package org.digma.intellij.plugin.ui.navigation

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.env.Env
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.navigation.MainContentViewSwitcher
import org.digma.intellij.plugin.navigation.View
import org.digma.intellij.plugin.ui.jcef.BaseMessageRouterHandler
import org.digma.intellij.plugin.ui.jcef.jsonToObject

class NavigationMessageRouterHandler(project: Project) : BaseMessageRouterHandler(project) {


    override fun getOriginForTroubleshootingEvent(): String {
        return "navigation"
    }


    override fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String, action: String) {

        Log.log(logger::trace, project, "got action '$action' with message $requestJsonNode")

        when (action) {

            "NAVIGATION/INITIALIZE" -> onInitialize(browser)

            "NAVIGATION/CHANGE_VIEW" -> changeView(requestJsonNode)

            "NAVIGATION/CHANGE_ENVIRONMENT" -> {
                changeEnvironment(requestJsonNode)
            }

            else -> {
                Log.log(logger::warn, "got unexpected action='$action'")
            }
        }
    }


    private fun changeEnvironment(requestJsonNode: JsonNode) {
        val payload = getPayloadFromRequest(requestJsonNode)
        payload?.takeIf { payload.get("environment") != null }?.let { pl ->
            val envAsString = objectMapper.writeValueAsString(pl.get("environment"))
            val env: Env = jsonToObject(envAsString, Env::class.java)
            AnalyticsService.getInstance(project).environment.setCurrent(env)
        }
    }


    private fun onInitialize(browser: CefBrowser) {
        try {
            doCommonInitialize(browser)
        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, e, "error getting backend info")
        }
    }


    private fun changeView(requestJsonNode: JsonNode) {
        val payload = getPayloadFromRequest(requestJsonNode)
        payload?.let {
            val viewId = payload.get("view").asText()
            val view = View.findById(viewId)
            view?.let {
                MainContentViewSwitcher.getInstance(project).showView(it, false)
            }
        }
    }

}
