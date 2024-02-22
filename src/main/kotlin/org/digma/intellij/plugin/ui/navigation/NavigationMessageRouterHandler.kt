package org.digma.intellij.plugin.ui.navigation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.navigation.MainContentViewSwitcher
import org.digma.intellij.plugin.navigation.View
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

            "NAVIGATION/GO_TO_CODE_LOCATION" -> {
                goToCode(requestJsonNode)
            }

            else -> {
                Log.log(logger::warn, "got unexpected action='$action'")
            }
        }
    }

    private fun goToCode(requestJsonNode: JsonNode) {
        val payload = getPayloadFromRequest(requestJsonNode)
        payload?.let { pl ->
            val codeDetails = pl.get("codeDetails")
            codeDetails?.takeIf { codeDetails !is NullNode }.let { cd ->
                val codeDetailsObj = objectMapper.readTree(cd.toString())
                val codeObjectId = codeDetailsObj.get("codeObjectId").asText()
                NavigationService.getInstance(project).navigateToCode(codeObjectId)
            }
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
            sendCurrentViewsState(browser, View.views, false)
        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, e, "error getting backend info")
        }
    }


    private fun changeView(requestJsonNode: JsonNode) {
        val payload = getPayloadFromRequest(requestJsonNode)
        payload?.let {
            val viewId = payload.get("view")?.asText()
            viewId?.let { vuid ->
                MainContentViewSwitcher.getInstance(project).showViewById(vuid, true)
            }
        }
    }


    private fun fixMissingDependencies(requestJsonNode: JsonNode) {
        val payload = getPayloadFromRequest(requestJsonNode)
        payload?.let {
            val methodId = payload.get("methodId").asText()
            NavigationService.getInstance(project).fixMissingDependencies(CodeObjectsUtil.stripMethodPrefix(methodId))
        }
    }

    private fun addAnnotation(requestJsonNode: JsonNode) {
        val payload = getPayloadFromRequest(requestJsonNode)
        payload?.let {
            val methodId = payload.get("methodId").asText()
            NavigationService.getInstance(project).addAnnotation(CodeObjectsUtil.stripMethodPrefix(methodId))
        }
    }

}
