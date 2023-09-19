package org.digma.intellij.plugin.ui.jcef

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils
import org.digma.intellij.plugin.ui.jcef.model.ConnectionStatusMessage
import org.digma.intellij.plugin.ui.jcef.model.IsDigmaRunningPayload


/**
 * try to find a field in the payload without throwing NPE.
 * requestJsonNode is the full request as json node
 */
fun tryGetFieldFromPayload(objectMapper: ObjectMapper, requestJsonNode: JsonNode, fieldName: String): String? {
    try {
        return objectMapper.readTree(requestJsonNode.get("payload").toString()).get(fieldName).asText()
    } catch (e: NullPointerException) {
        return null
    }
}


fun sendConnectionStatus(cefBrowser: CefBrowser, status: Boolean) {
    val connectionStatusMessage = ConnectionStatusMessage(
        JCefMessagesUtils.REQUEST_MESSAGE_TYPE,
        "GLOBAL/SET_IS_DIGMA_RUNNING", IsDigmaRunningPayload(status)
    )
    serializeAndExecuteWindowPostMessageJavaScript(cefBrowser, connectionStatusMessage)
}