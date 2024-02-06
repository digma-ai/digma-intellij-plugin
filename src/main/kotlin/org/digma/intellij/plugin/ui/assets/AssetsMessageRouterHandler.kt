package org.digma.intellij.plugin.ui.assets

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.ui.assets.model.SetAssetsDataFiltersMessage
import org.digma.intellij.plugin.ui.assets.model.SetAssetsDataMessage
import org.digma.intellij.plugin.ui.assets.model.SetCategoriesDataMessage
import org.digma.intellij.plugin.ui.assets.model.SetServicesDataMessage
import org.digma.intellij.plugin.ui.jcef.BaseMessageRouterHandler
import org.digma.intellij.plugin.ui.jcef.serializeAndExecuteWindowPostMessageJavaScript

class AssetsMessageRouterHandler(project: Project) : BaseMessageRouterHandler(project) {


    override fun getOriginForTroubleshootingEvent(): String {
        return "assets"
    }

    override fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String, action: String) {

        Log.log(logger::trace, project, "got action '$action' with message $requestJsonNode")

        when (action) {

            "ASSETS/INITIALIZE" -> onInitialize(browser)
            "ASSETS/GET_CATEGORIES_DATA" -> pushAssetCategories(browser, requestJsonNode)
            "ASSETS/GET_DATA" -> pushAssetsFromGetData(browser, requestJsonNode)
            "ASSETS/GET_ASSET_FILTERS_DATA" -> pushAssetFilters(browser, requestJsonNode)
            "ASSETS/GO_TO_ASSET" -> goToAsset(requestJsonNode)
            "ASSETS/GET_SERVICES" -> pushServices(browser)
            "ASSETS/SET_SELECTED_SERVICES" -> {
                val services = getServices(requestJsonNode)
                PersistenceService.getInstance().setSelectedServices(project.name, services)
            }

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


    @Synchronized
    @Throws(JsonProcessingException::class)
    private fun pushAssetCategories(browser: CefBrowser, requestJsonNode: JsonNode) {

        Log.log(logger::trace, project, "pushCategories called")

        val backendQueryParams = getQueryMapFromPayload(requestJsonNode)
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

        val backendQueryParams: Map<String, Any> = getQueryMapFromPayload(requestJsonNode)
        Log.log(logger::trace, project, "pushAssets called")
        val payload = objectMapper.readTree(AssetsService.getInstance(project).getAssets(backendQueryParams))
        val message = SetAssetsDataMessage(payload)
        Log.log(logger::trace, project, "sending ASSETS/SET_DATA message")
        serializeAndExecuteWindowPostMessageJavaScript(browser, message)
    }


    @Throws(JsonProcessingException::class)
    private fun pushAssetFilters(browser: CefBrowser, requestJsonNode: JsonNode) {

        val backendQueryParams: Map<String, Any> = getQueryMapFromPayload(requestJsonNode)

        Log.log(logger::trace, project, "pushAssetsFilters called")
        val payload = objectMapper.readTree(AssetsService.getInstance(project).getAssetFilters(backendQueryParams))
        val message = SetAssetsDataFiltersMessage(payload)
        Log.log(logger::trace, project, "sending ASSETS/SET_ASSET_FILTERS_DATA message")
        serializeAndExecuteWindowPostMessageJavaScript(browser, message)
    }


    @Throws(JsonProcessingException::class)
    private fun goToAsset(requestJsonNode: JsonNode) {
        Log.log(logger::trace, project, "got ASSETS/GO_TO_ASSET message")
        val spanId = objectMapper.readTree(requestJsonNode["payload"].toString())["spanCodeObjectId"].asText()
        Log.log(logger::trace, project, "got span id {}", spanId)
        AssetsService.getInstance(project).showAsset(spanId)
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


    private fun getQueryMapFromPayload(requestJsonNode: JsonNode): Map<String, Any> {

        val payloadNode: JsonNode = objectMapper.readTree(requestJsonNode.get("payload").toString())
        val payloadQuery: JsonNode = objectMapper.readTree(payloadNode.get("query").toString())

        val backendQueryParams = mutableMapOf<String, Any>()

        if (payloadQuery is ObjectNode) {

            val payloadQueryAsMap = objectMapper.convertValue(payloadQuery, Map::class.java)

            payloadQueryAsMap.forEach { entry: Map.Entry<Any?, Any?> ->
                entry.key?.let {
                    val value = entry.value
                    if (value is List<*>) {
                        backendQueryParams[it.toString()] = value.joinToString(",")
                    } else {
                        backendQueryParams[it.toString()] = value.toString()
                    }

                }
            }
        }

        backendQueryParams["environment"] = PersistenceService.getInstance().getCurrentEnv() ?: ""
        return backendQueryParams
    }
}
