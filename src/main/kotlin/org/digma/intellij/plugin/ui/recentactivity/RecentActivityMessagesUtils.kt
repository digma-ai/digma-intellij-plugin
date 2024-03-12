package org.digma.intellij.plugin.ui.recentactivity

import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.analytics.ConnectionTestResult
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants
import org.digma.intellij.plugin.ui.jcef.serializeAndExecuteWindowPostMessageJavaScript
import org.digma.intellij.plugin.ui.recentactivity.model.ConnectionTestResultMessage


fun sendRemoteConnectionCheckResult(browser: CefBrowser, connectionTestResult: ConnectionTestResult) {
    val connectionTestResultMessage = ConnectionTestResultMessage(
        JCEFGlobalConstants.REQUEST_MESSAGE_TYPE,
        "RECENT_ACTIVITY/SET_REMOTE_ENVIRONMENT_CONNECTION_CHECK_RESULT", connectionTestResult
    )
    serializeAndExecuteWindowPostMessageJavaScript(browser, connectionTestResultMessage)
}