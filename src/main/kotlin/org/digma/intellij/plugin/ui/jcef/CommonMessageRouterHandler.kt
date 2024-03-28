package org.digma.intellij.plugin.ui.jcef

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser


/**
 * common interface for classes that handle queries from a jcef app.
 */
interface CommonMessageRouterHandler {

    val objectMapper: ObjectMapper


    /**
     * each app router handler should implement this method for specific app messages.
     * return true if the action was handled. false if the action is unknown. used for error reporting only.
     */
    @Throws(Exception::class) // this is necessary for implementations in java that may throw exceptions
    fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String, action: String): Boolean


    fun getPayloadFromRequest(requestJsonNode: JsonNode): JsonNode? {
        val payload = requestJsonNode.get("payload")
        return payload?.let {
            objectMapper.readTree(it.toString())
        }
    }

    //NPE should be handled by caller
    @Throws(NullPointerException::class)
    fun getPayloadFromRequestNonNull(requestJsonNode: JsonNode): JsonNode {
        val payload = requestJsonNode.get("payload")
        return payload ?: throw NullPointerException("payload is null")
    }


    fun getEnvironmentIdFromPayload(requestJsonNode: JsonNode): String? {
        val payload = getPayloadFromRequest(requestJsonNode)
        return payload?.takeIf { payload.get("environment") != null }?.get("environment")?.asText()
    }
}

