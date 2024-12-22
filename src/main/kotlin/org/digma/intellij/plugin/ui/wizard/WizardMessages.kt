package org.digma.intellij.plugin.ui.wizard

import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants
import org.digma.intellij.plugin.ui.jcef.serializeAndExecuteWindowPostMessageJavaScript
import org.digma.intellij.plugin.ui.wizard.model.JcefDockerIsDigmaEngineInstalledPayload
import org.digma.intellij.plugin.ui.wizard.model.JcefDockerIsDigmaEngineInstalledRequest
import org.digma.intellij.plugin.ui.wizard.model.JcefDockerIsDigmaEngineRunningPayload
import org.digma.intellij.plugin.ui.wizard.model.JcefDockerIsDigmaEngineRunningRequest
import org.digma.intellij.plugin.ui.wizard.model.JcefDockerIsDockerComposeInstalledPayload
import org.digma.intellij.plugin.ui.wizard.model.JcefDockerIsDockerComposeInstalledRequest
import org.digma.intellij.plugin.ui.wizard.model.JcefDockerIsDockerInstalledPayload
import org.digma.intellij.plugin.ui.wizard.model.JcefDockerIsDockerInstalledRequest
import org.digma.intellij.plugin.ui.wizard.model.JcefDockerResultPayload
import org.digma.intellij.plugin.ui.wizard.model.JcefDockerResultRequest


fun sendIsDigmaEngineInstalled(result: Boolean, browser: CefBrowser) {

    val payload = JcefDockerIsDigmaEngineInstalledPayload(result)
    val message = JcefDockerIsDigmaEngineInstalledRequest(
        JCEFGlobalConstants.REQUEST_MESSAGE_TYPE,
        JCEFGlobalConstants.GLOBAL_SET_IS_DIGMA_ENGINE_INSTALLED,
        payload
    )
    serializeAndExecuteWindowPostMessageJavaScript(browser, message)
}


fun sendIsDigmaEngineRunning(success: Boolean, browser: CefBrowser) {

    val payload = JcefDockerIsDigmaEngineRunningPayload(success)
    val message = JcefDockerIsDigmaEngineRunningRequest(
        JCEFGlobalConstants.REQUEST_MESSAGE_TYPE,
        JCEFGlobalConstants.GLOBAL_SET_IS_DIGMA_ENGINE_RUNNING,
        payload
    )
    serializeAndExecuteWindowPostMessageJavaScript(browser, message)
}


fun sendDockerResult(result: String, errorMsg: String, browser: CefBrowser, messageType: String) {

    val payload = JcefDockerResultPayload(result, errorMsg)
    val message = JcefDockerResultRequest(
        JCEFGlobalConstants.REQUEST_MESSAGE_TYPE,
        messageType,
        payload
    )
    serializeAndExecuteWindowPostMessageJavaScript(browser, message)
}

fun sendIsDockerInstalled(result: Boolean, browser: CefBrowser) {

    val isDockerInstalledPayload = JcefDockerIsDockerInstalledPayload(result)
    val message = JcefDockerIsDockerInstalledRequest(
        JCEFGlobalConstants.REQUEST_MESSAGE_TYPE,
        JCEFGlobalConstants.GLOBAL_SET_IS_DOCKER_INSTALLED,
        isDockerInstalledPayload
    )
    serializeAndExecuteWindowPostMessageJavaScript(browser, message)
}

fun sendIsDockerComposeInstalled(result: Boolean, browser: CefBrowser) {

    val isDockerComposeInstalledPayload = JcefDockerIsDockerComposeInstalledPayload(result)
    val message = JcefDockerIsDockerComposeInstalledRequest(
        JCEFGlobalConstants.REQUEST_MESSAGE_TYPE,
        JCEFGlobalConstants.GLOBAL_SET_IS_DOCKER_COMPOSE_INSTALLED,
        isDockerComposeInstalledPayload
    )
    serializeAndExecuteWindowPostMessageJavaScript(browser, message)
}