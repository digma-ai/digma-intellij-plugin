package org.digma.intellij.plugin.ui.jcef

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.env.Env


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


    fun getEnvironmentFromPayload(requestJsonNode: JsonNode): Env? {
        val payload = getPayloadFromRequest(requestJsonNode)
        return payload?.takeIf { payload.get("environment") != null }?.let { pl ->
            val envAsString = objectMapper.writeValueAsString(pl.get("environment"))
            val env: Env = jsonToObject(envAsString, Env::class.java)
            env
        }
    }

}

