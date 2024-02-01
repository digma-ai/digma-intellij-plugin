package org.digma.intellij.plugin.ui.jcef

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.common.getEnvironmentEntities
import org.digma.intellij.plugin.docker.DigmaInstallationStatus
import org.digma.intellij.plugin.docker.DockerService
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.ui.jcef.model.ApiUrlPayload
import org.digma.intellij.plugin.ui.jcef.model.DigmaEngineStatusMessage
import org.digma.intellij.plugin.ui.jcef.model.IsMicrometerPayload
import org.digma.intellij.plugin.ui.jcef.model.SetApiUrlMessage
import org.digma.intellij.plugin.ui.jcef.model.SetEnvironmentMessage
import org.digma.intellij.plugin.ui.jcef.model.SetEnvironmentMessagePayload
import org.digma.intellij.plugin.ui.jcef.model.SetEnvironmentsMessage
import org.digma.intellij.plugin.ui.jcef.model.SetEnvironmentsMessagePayload
import org.digma.intellij.plugin.ui.jcef.model.SetIsMicrometerMessage
import org.digma.intellij.plugin.ui.jcef.model.SetUserEmailMessage
import org.digma.intellij.plugin.ui.jcef.model.UserEmailPayload


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
    val status = service<DockerService>().getActualRunningEngine(project)
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


fun sendApiUrl(cefBrowser: CefBrowser, url: String) {
    val setDigmaApiUrlMessage = SetApiUrlMessage(
        JCefMessagesUtils.REQUEST_MESSAGE_TYPE,
        "GLOBAL/SET_DIGMA_API_URL", ApiUrlPayload(url)
    )
    serializeAndExecuteWindowPostMessageJavaScript(cefBrowser, setDigmaApiUrlMessage)
}


fun sendIsMicrometerProject(cefBrowser: CefBrowser, isMicrometer: Boolean) {
    serializeAndExecuteWindowPostMessageJavaScript(
        cefBrowser,
        SetIsMicrometerMessage(IsMicrometerPayload(isMicrometer))
    )
}


fun sendUserEmail(cefBrowser: CefBrowser, email: String) {
    val setUserEmailMessage = SetUserEmailMessage(
        JCefMessagesUtils.REQUEST_MESSAGE_TYPE,
        "GLOBAL/SET_USER_REGISTRATION_EMAIL", UserEmailPayload(email)
    )
    serializeAndExecuteWindowPostMessageJavaScript(cefBrowser, setUserEmailMessage)
}

fun sendEnvironmentEntities(cefBrowser: CefBrowser, environments: List<String>) {
    val environmentEntities = getEnvironmentEntities(environments)
    serializeAndExecuteWindowPostMessageJavaScript(
        cefBrowser,
        SetEnvironmentsMessage(SetEnvironmentsMessagePayload(environmentEntities))
    )
}

fun sendCurrentEnvironment(cefBrowser: CefBrowser, environment: String?) {
    val envToSend = environment ?: PersistenceService.getInstance().getCurrentEnv() ?: ""
    serializeAndExecuteWindowPostMessageJavaScript(
        cefBrowser,
        SetEnvironmentMessage(SetEnvironmentMessagePayload(envToSend))
    )
}