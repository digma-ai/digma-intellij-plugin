package org.digma.intellij.plugin.ui.jcef

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.log.Log

val logger = Logger.getInstance("org.digma.intellij.plugin.ui.jcef.JCefBrowserUtils")


fun serializeObjectToJson(value: Any): String {
    return CommonObjectMapper.objectMapper.writeValueAsString(value)
}

//if possible better to use the method variant with a project
fun serializeAndExecuteWindowPostMessageJavaScript(browser: CefBrowser, message: Any) {
    executeWindowPostMessageJavaScript(browser, CommonObjectMapper.objectMapper.writeValueAsString(message))
}

//we need the project for debugging purposes only
fun serializeAndExecuteWindowPostMessageJavaScript(browser: CefBrowser, message: Any, project: Project) {
    executeWindowPostMessageJavaScript(browser, CommonObjectMapper.objectMapper.writeValueAsString(message), project)
}


fun executeWindowPostMessageJavaScript(browser: CefBrowser, message: String, project: Project? = null) {

    Log.log(logger::trace, "sending message to jcef app {}, message {}, project {}", browser.url, message, project?.name)

    browser.executeJavaScript(
        "window.postMessage($message);",
        browser.url,
        0
    )
}


fun <T> jsonToObject(jsonStr: String, type: Class<T>): T {
    return CommonObjectMapper.objectMapper.readValue(jsonStr, type)
}

fun <T> jsonToObject(jsonNode: JsonNode, type: Class<T>): T {
    return CommonObjectMapper.objectMapper.treeToValue(jsonNode, type)
}
