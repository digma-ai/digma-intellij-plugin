package org.digma.intellij.plugin.ui.errors

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.errors.model.SetErrorTimeSeriesDataMessage
import org.digma.intellij.plugin.ui.errors.model.SetErrorsDataMessage
import org.digma.intellij.plugin.ui.errors.model.SetErrorsDetailsMessage
import org.digma.intellij.plugin.ui.errors.model.SetErrorsDismissMessage
import org.digma.intellij.plugin.ui.errors.model.SetErrorsPinMessage
import org.digma.intellij.plugin.ui.errors.model.SetErrorsUndismissMessage
import org.digma.intellij.plugin.ui.errors.model.SetErrorsUnpinMessage
import org.digma.intellij.plugin.ui.errors.model.SetFilesUrlsMessage
import org.digma.intellij.plugin.ui.errors.model.SetGlobalErrorsDataMessage
import org.digma.intellij.plugin.ui.errors.model.SetGlobalErrorsFiltersDataMessage
import org.digma.intellij.plugin.ui.jcef.BaseCommonMessageRouterHandler
import org.digma.intellij.plugin.ui.jcef.getMapFromNode
import org.digma.intellij.plugin.ui.jcef.serializeAndExecuteWindowPostMessageJavaScript

class ErrorsMessageRouterHandler(project: Project) : BaseCommonMessageRouterHandler(project) {

    override fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String, action: String): Boolean {

        Log.log(logger::trace, project, "got action '$action' with message $requestJsonNode")

        when (action) {
            "ERRORS/GET_ERRORS_DATA" -> getErrorsData(project, browser, requestJsonNode)
            "ERRORS/GET_GLOBAL_ERRORS_DATA" -> getGlobalErrorsData(project, browser, requestJsonNode)
            "ERRORS/GET_GLOBAL_ERRORS_FILTERS_DATA" -> getGlobalErrorsFiltersData(project, browser, requestJsonNode)
            "ERRORS/GET_ERROR_TIME_SERIES_DATA" -> getErrorTimeSeriesData(project, browser, requestJsonNode)
            "ERRORS/GET_ERROR_DETAILS" -> getErrorDetails(project, browser, requestJsonNode)
            "ERRORS/OPEN_RAW_ERROR_STACK_TRACE_IN_EDITOR" -> openStackTrace(project, requestJsonNode)
            "ERRORS/GO_TO_CODE_LOCATION" -> navigateToCode(project, requestJsonNode)
            "ERRORS/GET_FILES_URIS" -> getFilesUrls(project, browser, requestJsonNode)
            "ERRORS/GO_TO_TRACE" -> goToTrace(project, requestJsonNode)
            "ERRORS/PIN_ERROR" -> pinError(project, browser, requestJsonNode)
            "ERRORS/UNPIN_ERROR" -> unpinError(project, browser, requestJsonNode)
            "ERRORS/DISMISS_ERROR" -> dismissError(project, browser, requestJsonNode)
            "ERRORS/UNDISMISS_ERROR" -> undismissError(project, browser, requestJsonNode)

            else -> return false
        }

        return true
    }



    private fun getErrorsData(project: Project, browser: CefBrowser, requestJsonNode: JsonNode) {
        getPayloadFromRequest(requestJsonNode)?.let { payload ->
            val spanCodeObjectId = payload.get("spanCodeObjectId")?.takeIf { it !is NullNode }?.asText()
            val methodId = payload.get("methodId")?.takeIf { it !is NullNode }?.asText()

            val objectIds = listOfNotNull(spanCodeObjectId, methodId)
            if (objectIds.isNotEmpty()) {
                val errorsData = objectMapper.readTree(ErrorsService.getInstance(project).getErrorsData(objectIds))
                val errorsDataWrapper = objectMapper.createObjectNode()
                errorsDataWrapper.set<JsonNode>("errors", errorsData)
                val setErrorsDataMessage = SetErrorsDataMessage(errorsDataWrapper)
                serializeAndExecuteWindowPostMessageJavaScript(browser, setErrorsDataMessage, project)
            }
        }
    }

    private fun getGlobalErrorsData(project: Project, browser: CefBrowser, requestJsonNode: JsonNode) {
        getPayloadFromRequest(requestJsonNode)?.let { payload ->
            val errorsData = ErrorsService.getInstance(project).getGlobalErrorsData(payload.toString())
            val setErrorsDataMessage = SetGlobalErrorsDataMessage(errorsData)
            serializeAndExecuteWindowPostMessageJavaScript(browser, setErrorsDataMessage, project)
        }
    }

    private fun pinError(project: Project, browser: CefBrowser, requestJsonNode: JsonNode) {
        getPayloadFromRequest(requestJsonNode)?.let { payload: JsonNode ->
            val errorId = payload.get("id")?.takeIf { it !is NullNode }?.asText() ?: ""
            val environment = payload.get("environment")?.takeIf { it !is NullNode }?.asText() ?: ""

            val pinResult = ErrorsService.getInstance(project).pinError(errorId, environment)
            val setErrorsDataMessage = SetErrorsPinMessage(pinResult)
            serializeAndExecuteWindowPostMessageJavaScript(browser, setErrorsDataMessage, project)
        }
    }

    private fun unpinError(project: Project, browser: CefBrowser, requestJsonNode: JsonNode) {
        getPayloadFromRequest(requestJsonNode)?.let { payload: JsonNode ->
            val errorId = payload.get("id")?.takeIf { it !is NullNode }?.asText() ?: ""
            val environment = payload.get("environment")?.takeIf { it !is NullNode }?.asText() ?: ""

            val pinResult = ErrorsService.getInstance(project).unpinError(errorId, environment)
            val setErrorsDataMessage = SetErrorsUnpinMessage(pinResult)
            serializeAndExecuteWindowPostMessageJavaScript(browser, setErrorsDataMessage, project)
        }
    }

    private fun dismissError(project: Project, browser: CefBrowser, requestJsonNode: JsonNode) {
        getPayloadFromRequest(requestJsonNode)?.let { payload: JsonNode ->
            val errorId = payload.get("id")?.takeIf { it !is NullNode }?.asText() ?: ""
            val environment = payload.get("environment")?.takeIf { it !is NullNode }?.asText() ?: ""

            val pinResult = ErrorsService.getInstance(project).dismissError(errorId, environment)
            val setErrorsDataMessage = SetErrorsDismissMessage(pinResult)
            serializeAndExecuteWindowPostMessageJavaScript(browser, setErrorsDataMessage, project)
        }
    }

    private fun undismissError(project: Project, browser: CefBrowser, requestJsonNode: JsonNode) {
        getPayloadFromRequest(requestJsonNode)?.let { payload: JsonNode ->
            val errorId = payload.get("id")?.takeIf { it !is NullNode }?.asText() ?: ""
            val environment = payload.get("environment")?.takeIf { it !is NullNode }?.asText() ?: ""

            val pinResult = ErrorsService.getInstance(project).undismissError(errorId, environment)
            val setErrorsDataMessage = SetErrorsUndismissMessage(pinResult)
            serializeAndExecuteWindowPostMessageJavaScript(browser, setErrorsDataMessage, project)
        }
    }

    private fun getErrorTimeSeriesData(project: Project, browser: CefBrowser, requestJsonNode: JsonNode) {
        getPayloadFromRequest(requestJsonNode)?.let { payload ->
            val errorId = payload.get("errorId")?.takeIf { it !is NullNode }?.asText()
            if (errorId != null) {
                var queryMap = getMapFromNode(payload.get("scope"), objectMapper)
                val errorsData = ErrorsService.getInstance(project).getErrorTimeseries(errorId, queryMap)
                val setErrorsDataMessage = SetErrorTimeSeriesDataMessage(errorsData)
                serializeAndExecuteWindowPostMessageJavaScript(browser, setErrorsDataMessage, project)
            }
        }
    }

    private fun getGlobalErrorsFiltersData(project: Project, browser: CefBrowser, requestJsonNode: JsonNode) {
        getPayloadFromRequest(requestJsonNode)?.let { payload ->
            val errorsData = ErrorsService.getInstance(project).getGlobalErrorsFiltersData(payload.toString())
            val setErrorsDataMessage = SetGlobalErrorsFiltersDataMessage(errorsData)
            serializeAndExecuteWindowPostMessageJavaScript(browser, setErrorsDataMessage, project)
        }
    }

    private fun getErrorDetails(project: Project, browser: CefBrowser, requestJsonNode: JsonNode) {
        getPayloadFromRequest(requestJsonNode)?.let { payload ->
            val errorId = payload.get("errorId")?.takeIf { it !is NullNode }?.asText()
            if (errorId != null) {
                val errorDetails = objectMapper.readTree(ErrorsService.getInstance(project).getErrorDetails(errorId))
                val errorDetailsWrapper = objectMapper.createObjectNode()
                errorDetailsWrapper.set<JsonNode>("details", errorDetails)
                val setErrorDetailsMessage = SetErrorsDetailsMessage(errorDetailsWrapper)
                serializeAndExecuteWindowPostMessageJavaScript(browser, setErrorDetailsMessage, project)
            }
        }
    }

    private fun openStackTrace(project: Project, requestJsonNode: JsonNode) {
        getPayloadFromRequest(requestJsonNode)?.let { payload ->
            val stackTrace = payload.get("stackTrace")?.takeIf { it !is NullNode }?.asText()
            if (stackTrace != null) {
                ErrorsService.getInstance(project).openRawStackTrace(stackTrace)
            }

        }
    }

    private fun navigateToCode(project: Project, requestJsonNode: JsonNode) {
        getPayloadFromRequest(requestJsonNode)?.let { payload ->
            val uri = payload.get("URI")?.takeIf { it !is NullNode }?.asText()
            val lineNumber = payload.get("lineNumber")?.takeIf { it !is NullNode }?.asText() ?: "1"
            val lastInstanceCommitId = payload.get("lastInstanceCommitId")?.takeIf { it !is NullNode }?.asText()

            if (uri != null) {
                ErrorsService.getInstance(project).openErrorFrameWorkspaceFile(uri, lineNumber, lastInstanceCommitId)
            }
        }
    }

    private fun getFilesUrls(project: Project, browser: CefBrowser, requestJsonNode: JsonNode) {
        getPayloadFromRequest(requestJsonNode)?.let { payload ->

            val codeObjectIds = payload.get("codeObjectIds")
            val reader: ObjectReader = objectMapper.readerFor(object : TypeReference<List<String>>() {})
            val list: List<String> = reader.readValue(codeObjectIds)

            val workspaceUrls = ErrorsService.getInstance(project).getWorkspaceUris(list)

            val filesUrlsWrapper = objectMapper.createObjectNode()
            filesUrlsWrapper.set<ObjectNode>("filesURIs", objectMapper.valueToTree(workspaceUrls))
            val setFilesUrlsMessage = SetFilesUrlsMessage(filesUrlsWrapper)
            serializeAndExecuteWindowPostMessageJavaScript(browser, setFilesUrlsMessage, project)

        }
    }


    private fun goToTrace(project: Project, requestJsonNode: JsonNode) {
        getPayloadFromRequest(requestJsonNode)?.let { payload ->
            val traceId = payload.get("traceId")?.takeIf { it !is NullNode }?.asText()
            val spanName = payload.get("spanName")?.takeIf { it !is NullNode }?.asText()
            val spanCodeObjectId = payload.get("spanCodeObjectId")?.takeIf { it !is NullNode }?.asText()

            if (traceId != null && spanName != null) {
                ErrorsService.getInstance(project).openTrace(traceId, spanName, spanCodeObjectId)
            }
        }
    }

}