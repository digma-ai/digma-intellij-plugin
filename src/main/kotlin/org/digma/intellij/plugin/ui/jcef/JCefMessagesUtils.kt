package org.digma.intellij.plugin.ui.jcef

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils
import org.digma.intellij.plugin.ui.jcef.model.DigmaEngineStatusMessage


/**
 * try to find a field in the payload without throwing NPE.
 * requestJsonNode is the full request as json node
 */
fun tryGetFieldFromPayload(objectMapper: ObjectMapper, requestJsonNode: JsonNode, fieldName: String): String? {
    return try {
        objectMapper.readTree(requestJsonNode.get("payload").toString()).get(fieldName).asText()
    } catch (e: NullPointerException) {
        null
    }
}


fun sendDigmaEngineStatus(cefBrowser: CefBrowser, status: String) {

    val connectionStatusMessage = DigmaEngineStatusMessage(
        JCefMessagesUtils.REQUEST_MESSAGE_TYPE,
        "GLOBAL/SET_DIGMA_STATUS", status
    )
    serializeAndExecuteWindowPostMessageJavaScript(cefBrowser, connectionStatusMessage)
}