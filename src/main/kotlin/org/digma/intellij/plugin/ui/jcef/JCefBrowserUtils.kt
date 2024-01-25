package org.digma.intellij.plugin.ui.jcef

import org.cef.browser.CefBrowser


private object CommonObjectMapper {
    var objectMapper = createObjectMapper()
}


fun serializeAndExecuteWindowPostMessageJavaScript(browser: CefBrowser, message: Any) {
    executeWindowPostMessageJavaScript(browser, CommonObjectMapper.objectMapper.writeValueAsString(message))
}


fun executeWindowPostMessageJavaScript(browser: CefBrowser, message: String) {

    browser.executeJavaScript(
        "window.postMessage($message);",
        browser.url,
        0
    )
}


fun <T> jsonToObject(jsonStr: String, type: Class<T>): T {
    return CommonObjectMapper.objectMapper.readValue(jsonStr, type)
}
