package org.digma.intellij.plugin.ui.navigation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.navigation.MainContentViewSwitcher
import org.digma.intellij.plugin.navigation.View
import org.digma.intellij.plugin.scope.ScopeManager
import org.digma.intellij.plugin.scope.SpanScope
import org.digma.intellij.plugin.ui.jcef.BaseMessageRouterHandler

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

            "NAVIGATION/AUTOFIX_MISSING_DEPENDENCY" -> {
                fixMissingDependencies(requestJsonNode)
            }

            "NAVIGATION/ADD_ANNOTATION" -> {
                addAnnotation(requestJsonNode)
            }

            "NAVIGATION/CHANGE_SCOPE" -> {
                changeScope(requestJsonNode)
            }

            else -> {
                Log.log(logger::warn, "got unexpected action='$action'")
            }
        }
    }

    private fun changeScope(requestJsonNode: JsonNode) {
        val payload = getPayloadFromRequest(requestJsonNode)
        payload?.let { pl ->
            val span = pl.get("span")
            val spanScope: SpanScope? = span?.takeIf { span !is NullNode }?.let { sp ->
                val spanObj = objectMapper.readTree(sp.asText())
                val spanId = spanObj.get("spanCodeObjectId").asText()
                val serviceName = spanObj.get("serviceName").asText()
                SpanScope(spanId, null, serviceName, null)
            }

            spanScope?.let {
                ScopeManager.getInstance(project).changeScope(it)
            } ?: ScopeManager.getInstance(project).changeToHome()

        }
    }


    private fun changeEnvironment(requestJsonNode: JsonNode) {
        val environment = getEnvironmentFromPayload(requestJsonNode)
        environment?.let { env ->
            AnalyticsService.getInstance(project).environment.setCurrent(env)
        }
    }


    private fun onInitialize(browser: CefBrowser) {
        try {
            doCommonInitialize(browser)
            sendCurrentViewsState(browser, View.views)
        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, e, "error getting backend info")
        }
    }


    private fun changeView(requestJsonNode: JsonNode) {
        val payload = getPayloadFromRequest(requestJsonNode)
        payload?.let {
            val viewId = payload.get("view")?.asText()
            viewId?.let { vuid ->
                MainContentViewSwitcher.getInstance(project).showViewById(vuid)
            }
        }
    }


    private fun fixMissingDependencies(requestJsonNode: JsonNode) {
        val payload = getPayloadFromRequest(requestJsonNode)
        payload?.let {
            val methodId = payload.get("methodId").asText()
            NavigationService.getInstance(project).fixMissingDependencies(methodId)
        }
    }

    private fun addAnnotation(requestJsonNode: JsonNode) {
        val payload = getPayloadFromRequest(requestJsonNode)
        payload?.let {
            val methodId = payload.get("methodId").asText()
            NavigationService.getInstance(project).addAnnotation(methodId)
        }
    }

}
