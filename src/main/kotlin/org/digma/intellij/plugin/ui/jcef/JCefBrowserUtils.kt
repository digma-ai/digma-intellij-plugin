package org.digma.intellij.plugin.ui.jcef

import com.intellij.openapi.diagnostic.Logger
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.common.createObjectMapper
import org.digma.intellij.plugin.log.Log

val logger = Logger.getInstance("org.digma.intellij.plugin.ui.jcef.JCefBrowserUtils")

private object CommonObjectMapper {
    var objectMapper = createObjectMapper()
}


fun serializeObjectToJson(value: Any): String {
    return CommonObjectMapper.objectMapper.writeValueAsString(value)
}

fun serializeAndExecuteWindowPostMessageJavaScript(browser: CefBrowser, message: Any) {
    executeWindowPostMessageJavaScript(browser, CommonObjectMapper.objectMapper.writeValueAsString(message))
}


fun executeWindowPostMessageJavaScript(browser: CefBrowser, message: String) {

    Log.log(logger::trace, "sending message to jcef app {}, message {}", browser.url, message)

    browser.executeJavaScript(
        "window.postMessage($message);",
        browser.url,
        0
    )
}


fun <T> jsonToObject(jsonStr: String, type: Class<T>): T {
    return CommonObjectMapper.objectMapper.readValue(jsonStr, type)
}
