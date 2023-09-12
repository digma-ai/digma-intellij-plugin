package org.digma.intellij.plugin.ui.jcef

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.docker.DigmaInstallationStatus
import org.digma.intellij.plugin.docker.DockerService
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


fun updateDigmaEngineStatus(project: Project, cefBrowser: CefBrowser) {
    val status = service<DockerService>().getCurrentDigmaInstallationStatus(project)
    updateDigmaEngineStatus(cefBrowser, status)
}

fun updateDigmaEngineStatus(cefBrowser: CefBrowser, status: DigmaInstallationStatus) {
    sendDigmaEngineStatus(cefBrowser, status)
}


private fun sendDigmaEngineStatus(cefBrowser: CefBrowser, status: DigmaInstallationStatus) {

    val connectionStatusMessage = DigmaEngineStatusMessage(
        JCefMessagesUtils.REQUEST_MESSAGE_TYPE,
        "GLOBAL/SET_DIGMA_STATUS", status
    )
    serializeAndExecuteWindowPostMessageJavaScript(cefBrowser, connectionStatusMessage)
}