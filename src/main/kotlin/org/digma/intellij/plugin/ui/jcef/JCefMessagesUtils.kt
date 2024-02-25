package org.digma.intellij.plugin.ui.jcef

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.docker.DigmaInstallationStatus
import org.digma.intellij.plugin.docker.DockerService
import org.digma.intellij.plugin.env.Env
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils
import org.digma.intellij.plugin.model.rest.navigation.CodeLocation
import org.digma.intellij.plugin.scope.SpanScope
import org.digma.intellij.plugin.ui.jcef.model.ApiUrlPayload
import org.digma.intellij.plugin.ui.jcef.model.DigmaEngineStatusMessage
import org.digma.intellij.plugin.ui.jcef.model.IsJaegerButtonEnabledMessage
import org.digma.intellij.plugin.ui.jcef.model.IsJaegerButtonEnabledMessagePayload
import org.digma.intellij.plugin.ui.jcef.model.IsMicrometerPayload
import org.digma.intellij.plugin.ui.jcef.model.IsObservabilityEnabledMessage
import org.digma.intellij.plugin.ui.jcef.model.IsObservabilityEnabledPayload
import org.digma.intellij.plugin.ui.jcef.model.SetApiUrlMessage
import org.digma.intellij.plugin.ui.jcef.model.SetEnvironmentMessage
import org.digma.intellij.plugin.ui.jcef.model.SetEnvironmentMessagePayload
import org.digma.intellij.plugin.ui.jcef.model.SetEnvironmentsMessage
import org.digma.intellij.plugin.ui.jcef.model.SetEnvironmentsMessagePayload
import org.digma.intellij.plugin.ui.jcef.model.SetIsMicrometerMessage
import org.digma.intellij.plugin.ui.jcef.model.SetScopeMessage
import org.digma.intellij.plugin.ui.jcef.model.SetScopeMessagePayload
import org.digma.intellij.plugin.ui.jcef.model.SetStateMessage
import org.digma.intellij.plugin.ui.jcef.model.SetUserEmailMessage
import org.digma.intellij.plugin.ui.jcef.model.UserEmailPayload
import org.digma.intellij.plugin.ui.list.insights.isJaegerButtonEnabled


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

fun sendEnvironmentsList(cefBrowser: CefBrowser, environments: List<Env>) {
    serializeAndExecuteWindowPostMessageJavaScript(
        cefBrowser,
        SetEnvironmentsMessage(SetEnvironmentsMessagePayload(environments))
    )
}

fun sendCurrentEnvironment(cefBrowser: CefBrowser, environment: Env) {
    serializeAndExecuteWindowPostMessageJavaScript(
        cefBrowser,
        SetEnvironmentMessage(SetEnvironmentMessagePayload(environment))
    )
}


fun sendObservabilityEnabledMessage(cefBrowser: CefBrowser, isObservabilityEnabled: Boolean) {
    serializeAndExecuteWindowPostMessageJavaScript(
        cefBrowser,
        IsObservabilityEnabledMessage(IsObservabilityEnabledPayload(isObservabilityEnabled))
    )
}

fun sendScopeChangedMessage(
    cefBrowser: CefBrowser,
    scope: SpanScope?, codeLocation: CodeLocation, hasErrors: Boolean,
) {
    serializeAndExecuteWindowPostMessageJavaScript(
        cefBrowser, SetScopeMessage(SetScopeMessagePayload(scope, codeLocation, hasErrors))
    )
}

fun sendJcefStateMessage(cefBrowser: CefBrowser, state: JsonNode?) {
    serializeAndExecuteWindowPostMessageJavaScript(
        cefBrowser, SetStateMessage(state)
    )
}

fun sendIsJaegerButtonEnabledMessage(cefBrowser: CefBrowser) {
    serializeAndExecuteWindowPostMessageJavaScript(
        cefBrowser, IsJaegerButtonEnabledMessage(IsJaegerButtonEnabledMessagePayload(isJaegerButtonEnabled()))
    )
}

