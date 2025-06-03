package org.digma.intellij.plugin.ui.assets

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.assets.model.SetAssetsDataFiltersMessage
import org.digma.intellij.plugin.ui.assets.model.SetAssetsDataMessage
import org.digma.intellij.plugin.ui.assets.model.SetCategoriesDataMessage
import org.digma.intellij.plugin.ui.jcef.BaseCommonMessageRouterHandler
import org.digma.intellij.plugin.ui.jcef.getQueryMapFromPayload
import org.digma.intellij.plugin.ui.jcef.model.ErrorPayload
import org.digma.intellij.plugin.ui.jcef.serializeAndExecuteWindowPostMessageJavaScript

class AssetsMessageRouterHandler(project: Project) : BaseCommonMessageRouterHandler(project) {


    override suspend fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String, action: String): Boolean {

        Log.log(logger::trace, project, "got action '$action' with message $requestJsonNode")

        when (action) {

            "ASSETS/GET_CATEGORIES_DATA" -> pushAssetCategories(browser, requestJsonNode)
            "ASSETS/GET_DATA" -> pushAssetsFromGetData(browser, requestJsonNode)
            "ASSETS/GET_ASSET_FILTERS_DATA" -> pushAssetFilters(browser, requestJsonNode)

            else -> return false
        }

        return true
    }


    @Synchronized
    @Throws(JsonProcessingException::class)
    private fun pushAssetCategories(browser: CefBrowser, requestJsonNode: JsonNode) {

        Log.log(logger::trace, project, "pushCategories called")

        val message = try {
            val backendQueryParams = getQueryMapFromPayload(requestJsonNode, objectMapper)
            val payload = objectMapper.readTree(AssetsService.getInstance(project).getAssetCategories(backendQueryParams))
            SetCategoriesDataMessage(payload)
        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, project, e, "Error loading categories {}", e.message)
            val error = ErrorPayload(e.nonNullMessage)
            val payload = objectMapper.readTree("{ \"assetCategories\": [] }")
            SetCategoriesDataMessage(payload, error)
        }
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

        Log.log(logger::trace, project, "pushAssets called")
        val message = try{
            val backendQueryParams = getQueryMapFromPayload(requestJsonNode, objectMapper)
            val assets = AssetsService.getInstance(project).getAssets(backendQueryParams)
            val payload = objectMapper.readTree(assets)
            SetAssetsDataMessage(payload)
        }catch (e: AnalyticsServiceException){
            Log.warnWithException(logger,project,e,"Error loading assets {}",e)
            val error = ErrorPayload(e.nonNullMessage)
            val payload = objectMapper.readTree("")
            SetAssetsDataMessage(payload,error)
        }
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

}
