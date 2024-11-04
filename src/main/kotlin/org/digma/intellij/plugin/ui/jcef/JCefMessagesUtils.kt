package org.digma.intellij.plugin.ui.jcef

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefBrowser
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.analytics.BackendInfoHolder
import org.digma.intellij.plugin.docker.DigmaInstallationStatus
import org.digma.intellij.plugin.docker.LocalInstallationFacade
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.model.rest.AboutResult
import org.digma.intellij.plugin.model.rest.environment.Env
import org.digma.intellij.plugin.model.rest.insights.SpanEnvironment
import org.digma.intellij.plugin.model.rest.navigation.CodeLocation
import org.digma.intellij.plugin.model.rest.version.BackendDeploymentType
import org.digma.intellij.plugin.scope.ScopeContext
import org.digma.intellij.plugin.scope.SpanScope
import org.digma.intellij.plugin.ui.common.isJaegerButtonEnabled
import org.digma.intellij.plugin.ui.jcef.model.ApiUrlPayload
import org.digma.intellij.plugin.ui.jcef.model.BackendInfoMessage
import org.digma.intellij.plugin.ui.jcef.model.DigmaEngineStatusMessage
import org.digma.intellij.plugin.ui.jcef.model.DigmathonProductKeyPayload
import org.digma.intellij.plugin.ui.jcef.model.DigmathonStatePayload
import org.digma.intellij.plugin.ui.jcef.model.GenericPluginEventMessage
import org.digma.intellij.plugin.ui.jcef.model.GenericPluginEventPayload
import org.digma.intellij.plugin.ui.jcef.model.IsJaegerButtonEnabledMessage
import org.digma.intellij.plugin.ui.jcef.model.IsJaegerButtonEnabledMessagePayload
import org.digma.intellij.plugin.ui.jcef.model.IsMicrometerPayload
import org.digma.intellij.plugin.ui.jcef.model.IsObservabilityEnabledMessage
import org.digma.intellij.plugin.ui.jcef.model.IsObservabilityEnabledPayload
import org.digma.intellij.plugin.ui.jcef.model.RunConfigurationAttributesPayload
import org.digma.intellij.plugin.ui.jcef.model.SetApiUrlMessage
import org.digma.intellij.plugin.ui.jcef.model.SetDigmathonProductKey
import org.digma.intellij.plugin.ui.jcef.model.SetDigmathonState
import org.digma.intellij.plugin.ui.jcef.model.SetEnvironmentsMessage
import org.digma.intellij.plugin.ui.jcef.model.SetEnvironmentsMessagePayload
import org.digma.intellij.plugin.ui.jcef.model.SetInsightStatsMessage
import org.digma.intellij.plugin.ui.jcef.model.SetInsightStatsMessagePayload
import org.digma.intellij.plugin.ui.jcef.model.SetIsMicrometerMessage
import org.digma.intellij.plugin.ui.jcef.model.SetRunConfigurationAttributesMessage
import org.digma.intellij.plugin.ui.jcef.model.SetScopeMessage
import org.digma.intellij.plugin.ui.jcef.model.SetScopeMessagePayload
import org.digma.intellij.plugin.ui.jcef.model.SetStateMessage
import org.digma.intellij.plugin.ui.jcef.model.SetUserEmailMessage
import org.digma.intellij.plugin.ui.jcef.model.SetUserFinishedDigmathon
import org.digma.intellij.plugin.ui.jcef.model.SetUserInfoMessage
import org.digma.intellij.plugin.ui.jcef.model.UICodeFontRequest
import org.digma.intellij.plugin.ui.jcef.model.UIFontRequest
import org.digma.intellij.plugin.ui.jcef.model.UIThemeRequest
import org.digma.intellij.plugin.ui.jcef.model.UiCodeFontPayload
import org.digma.intellij.plugin.ui.jcef.model.UiFontPayload
import org.digma.intellij.plugin.ui.jcef.model.UiThemePayload
import org.digma.intellij.plugin.ui.jcef.model.UserEmailPayload
import org.digma.intellij.plugin.ui.jcef.model.UserFinishedDigmathonPayload
import org.digma.intellij.plugin.ui.jcef.model.UserInfoPayload
import org.digma.intellij.plugin.ui.settings.Theme


fun sendRequestToChangeUiTheme(uiTheme: Theme, jbCefBrowser: JBCefBrowser) {
    val message = UIThemeRequest(
        JCEFGlobalConstants.REQUEST_MESSAGE_TYPE,
        JCEFGlobalConstants.GLOBAL_SET_UI_THEME,
        UiThemePayload(uiTheme.themeName)
    )
    serializeAndExecuteWindowPostMessageJavaScript(jbCefBrowser.cefBrowser, message)
}


fun sendRequestToChangeFont(font: String?, jbCefBrowser: JBCefBrowser) {
    val message = UIFontRequest(
        JCEFGlobalConstants.REQUEST_MESSAGE_TYPE,
        JCEFGlobalConstants.GLOBAL_SET_UI_MAIN_FONT,
        UiFontPayload(font!!)
    )
    serializeAndExecuteWindowPostMessageJavaScript(jbCefBrowser.cefBrowser, message)
}

fun sendRequestToChangeCodeFont(font: String?, jbCefBrowser: JBCefBrowser) {
    val message = UICodeFontRequest(
        JCEFGlobalConstants.REQUEST_MESSAGE_TYPE,
        JCEFGlobalConstants.GLOBAL_SET_UI_CODE_FONT,
        UiCodeFontPayload(font!!)
    )
    serializeAndExecuteWindowPostMessageJavaScript(jbCefBrowser.cefBrowser, message)
}


fun sendDigmathonState(isActive: Boolean, cefBrowser: CefBrowser) {
    serializeAndExecuteWindowPostMessageJavaScript(
        cefBrowser,
        SetDigmathonState(DigmathonStatePayload(isActive))
    )
}

fun sendDigmathonProductKey(productKey: String?, cefBrowser: CefBrowser) {
    serializeAndExecuteWindowPostMessageJavaScript(
        cefBrowser,
        SetDigmathonProductKey(DigmathonProductKeyPayload(productKey))
    )
}

fun sendUserFinishedDigmathon(cefBrowser: CefBrowser) {
    serializeAndExecuteWindowPostMessageJavaScript(
        cefBrowser,
        SetUserFinishedDigmathon(UserFinishedDigmathonPayload(true))
    )
}


fun updateDigmaEngineStatus(project: Project, cefBrowser: CefBrowser) {
    try {
        val status = service<LocalInstallationFacade>().getDigmaInstallationStatus(project)
        updateDigmaEngineStatus(cefBrowser, status)
    } catch (e: Throwable) {
        ErrorReporter.getInstance().reportError("updateDigmaEngineStatus", e)
    }
}

fun updateDigmaEngineStatus(cefBrowser: CefBrowser, status: DigmaInstallationStatus) {
    sendDigmaEngineStatus(cefBrowser, status)
}

fun sendBackendAboutInfo(cefBrowser: CefBrowser, project: Project) {
    val about = BackendInfoHolder.getInstance(project).getAbout() ?: AboutResult("unknown", BackendDeploymentType.Unknown, false, null)
    val message = BackendInfoMessage(about)
    serializeAndExecuteWindowPostMessageJavaScript(cefBrowser, message, project)
}


private fun sendDigmaEngineStatus(cefBrowser: CefBrowser, status: DigmaInstallationStatus) {

    val connectionStatusMessage = DigmaEngineStatusMessage(
        JCEFGlobalConstants.REQUEST_MESSAGE_TYPE,
        "GLOBAL/SET_DIGMA_STATUS", status
    )
    serializeAndExecuteWindowPostMessageJavaScript(cefBrowser, connectionStatusMessage)
}


fun sendApiUrl(cefBrowser: CefBrowser, url: String) {
    val setDigmaApiUrlMessage = SetApiUrlMessage(
        JCEFGlobalConstants.REQUEST_MESSAGE_TYPE,
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
        JCEFGlobalConstants.REQUEST_MESSAGE_TYPE,
        "GLOBAL/SET_USER_REGISTRATION_EMAIL", UserEmailPayload(email)
    )
    serializeAndExecuteWindowPostMessageJavaScript(cefBrowser, setUserEmailMessage)
}

fun sendUserInfoMessage(cefBrowser: CefBrowser, userId: String?, project: Project) {
    val setUserEmailMessage = SetUserInfoMessage(UserInfoPayload(userId))
    serializeAndExecuteWindowPostMessageJavaScript(cefBrowser, setUserEmailMessage, project)
}

fun sendEnvironmentsList(cefBrowser: CefBrowser, environments: List<Env>) {
    try {
        serializeAndExecuteWindowPostMessageJavaScript(
            cefBrowser,
            SetEnvironmentsMessage(SetEnvironmentsMessagePayload(environments))
        )
    } catch (e: Throwable) {
        ErrorReporter.getInstance().reportError("sendEnvironmentsList", e)
    }
}


fun sendObservabilityEnabledMessage(cefBrowser: CefBrowser, isObservabilityEnabled: Boolean) {
    serializeAndExecuteWindowPostMessageJavaScript(
        cefBrowser,
        IsObservabilityEnabledMessage(IsObservabilityEnabledPayload(isObservabilityEnabled))
    )
}

fun sendScopeChangedMessage(
    cefBrowser: CefBrowser,
    scope: SpanScope?,
    codeLocation: CodeLocation,
    hasErrors: Boolean,
    analyticsInsightsCount: Number,
    issuesInsightsCount: Number,
    unreadInsightsCount: Number,
    scopeContext: ScopeContext?,
    environmentId: String?
) {
    serializeAndExecuteWindowPostMessageJavaScript(
        cefBrowser,
        SetScopeMessage(
            SetScopeMessagePayload(
                scope,
                codeLocation,
                hasErrors,
                analyticsInsightsCount,
                issuesInsightsCount,
                unreadInsightsCount,
                scopeContext,
                environmentId
            )
        )
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


fun sendSetInsightStatsMessage(
    cefBrowser: CefBrowser,
    scope: JsonNode?,
    analyticsInsightsCount: Number,
    issuesInsightsCount: Number,
    unreadInsightsCount: Number,
    criticalInsightsCount: Number,
    allIssuesCount: Number,
    spanEnvironments: List<SpanEnvironment> = listOf()
) {
    serializeAndExecuteWindowPostMessageJavaScript(
        cefBrowser,
        SetInsightStatsMessage(
            SetInsightStatsMessagePayload(
                scope,
                analyticsInsightsCount,
                issuesInsightsCount,
                unreadInsightsCount,
                criticalInsightsCount,
                allIssuesCount,
                spanEnvironments
            )
        )
    )
}


fun sendRunConfigurationAttributes(
    project: Project, cefBrowser: CefBrowser, payload: RunConfigurationAttributesPayload
) {
    val message = SetRunConfigurationAttributesMessage(payload)
    serializeAndExecuteWindowPostMessageJavaScript(cefBrowser, message, project)
}


fun sendGenericPluginEventStringPayload(project: Project, cefBrowser: CefBrowser, name: String, payload: String? = null) {
    val jsonNode: JsonNode? = payload?.let {
        try {
            CommonObjectMapper.objectMapper.readTree(payload)
        } catch (e: Throwable) {
            null
        }
    }
    sendGenericPluginEvent(project, cefBrowser, name, jsonNode)
}


fun sendGenericPluginEvent(project: Project, cefBrowser: CefBrowser, name: String, payload: JsonNode? = null) {
    val messagePayload = GenericPluginEventPayload(name, payload)
    val message = GenericPluginEventMessage(messagePayload)
    serializeAndExecuteWindowPostMessageJavaScript(cefBrowser, message, project)
}