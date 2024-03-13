package org.digma.intellij.plugin.ui.assets

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.ui.assets.model.SetAssetsDataFiltersMessage
import org.digma.intellij.plugin.ui.assets.model.SetAssetsDataMessage
import org.digma.intellij.plugin.ui.assets.model.SetCategoriesDataMessage
import org.digma.intellij.plugin.ui.assets.model.SetServicesDataMessage
import org.digma.intellij.plugin.ui.jcef.BaseCommonMessageRouterHandler
import org.digma.intellij.plugin.ui.jcef.getQueryMapFromPayload
import org.digma.intellij.plugin.ui.jcef.serializeAndExecuteWindowPostMessageJavaScript

class AssetsMessageRouterHandler(project: Project) : BaseCommonMessageRouterHandler(project) {


    override fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String, action: String): Boolean {

        Log.log(logger::trace, project, "got action '$action' with message $requestJsonNode")

        when (action) {

            "ASSETS/GET_CATEGORIES_DATA" -> pushAssetCategories(browser, requestJsonNode)
            "ASSETS/GET_DATA" -> pushAssetsFromGetData(browser, requestJsonNode)
            "ASSETS/GET_ASSET_FILTERS_DATA" -> pushAssetFilters(browser, requestJsonNode)
            "ASSETS/GO_TO_ASSET" -> goToAsset(requestJsonNode)
            "ASSETS/GET_SERVICES" -> pushServices(browser)
            "ASSETS/SET_SELECTED_SERVICES" -> {
                val services = getServices(requestJsonNode)
                PersistenceService.getInstance().setSelectedServices(project.name, services)
            }

            else -> return false
        }

        return true
    }


    @Synchronized
    @Throws(JsonProcessingException::class)
    private fun pushAssetCategories(browser: CefBrowser, requestJsonNode: JsonNode) {

        Log.log(logger::trace, project, "pushCategories called")

        val backendQueryParams = getQueryMapFromPayload(requestJsonNode, objectMapper)
        val payload = objectMapper.readTree(AssetsService.getInstance(project).getAssetCategories(backendQueryParams))
        val message = SetCategoriesDataMessage(payload)
        Log.log(logger::trace, project, "sending ASSETS/SET_CATEGORIES_DATA message")
        serializeAndExecuteWindowPostMessageJavaScript(browser, message)
    }


    @Throws(JsonProcessingException::class)
    private fun pushAssetsFromGetData(browser: CefBrowser, requestJsonNode: JsonNode) {
        Log.log(logger::trace, project, "got ASSETS/GET_DATA message")

        if (requestJsonNode.isMissingNode || requestJsonNode["payload"] == null) return

        pushAssets(browser, requestJsonNode)
    }

    @Synchronized
    @Throws(JsonProcessingException::class)
    private fun pushAssets(browser: CefBrowser, requestJsonNode: JsonNode) {

        val backendQueryParams = getQueryMapFromPayload(requestJsonNode, objectMapper)
        Log.log(logger::trace, project, "pushAssets called")
        val payload = objectMapper.readTree(AssetsService.getInstance(project).getAssets(backendQueryParams))
        val message = SetAssetsDataMessage(payload)
        Log.log(logger::trace, project, "sending ASSETS/SET_DATA message")
        serializeAndExecuteWindowPostMessageJavaScript(browser, message)
    }


    @Throws(JsonProcessingException::class)
    private fun pushAssetFilters(browser: CefBrowser, requestJsonNode: JsonNode) {

        val backendQueryParams = getQueryMapFromPayload(requestJsonNode, objectMapper)

        Log.log(logger::trace, project, "pushAssetsFilters called")
        val payload = objectMapper.readTree(AssetsService.getInstance(project).getAssetFilters(backendQueryParams))
        val message = SetAssetsDataFiltersMessage(payload)
        Log.log(logger::trace, project, "sending ASSETS/SET_ASSET_FILTERS_DATA message")
        serializeAndExecuteWindowPostMessageJavaScript(browser, message)
    }


    @Throws(JsonProcessingException::class)
    private fun goToAsset(requestJsonNode: JsonNode) {
        Log.log(logger::trace, project, "got ASSETS/GO_TO_ASSET message")
        val payload = getPayloadFromRequest(requestJsonNode)
        payload?.let { pl ->
            val spanId = pl.get("spanCodeObjectId").asText()
            Log.log(logger::trace, project, "got span id {}", spanId)
            AssetsService.getInstance(project).showAsset(spanId)
        }

    }


    @Throws(JsonProcessingException::class)
    private fun pushServices(browser: CefBrowser) {
        val servicesJsonString = AssetsService.getInstance(project).getServices()

        val services = if (servicesJsonString != null) {
            objectMapper.readTree(servicesJsonString)
        } else {
            objectMapper.createArrayNode()
        }

        val jNode = objectMapper.createObjectNode()
        jNode.set<JsonNode>("services", services)
        val message = SetServicesDataMessage(jNode)
        Log.log(logger::trace, project, "sending ASSETS/SET_SERVICES message")
        serializeAndExecuteWindowPostMessageJavaScript(browser, message)
    }


    private fun getServices(requestJsonNode: JsonNode): Array<String>? {
        var services: Array<String>? = null
        val payloadNode = requestJsonNode["payload"]

        payloadNode?.let {
            var node = payloadNode["services"]

            if (node == null) {
                node = payloadNode["query"]["services"]
            }

            if (node != null && node.isArray && node.elements().hasNext()) {
                services = objectMapper.convertValue(node, Array<String>::class.java)
            }
        }

        return services
    }
}
