package org.digma.intellij.plugin.ui.jcef

import org.cef.browser.CefBrowser


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

    browser.executeJavaScript(
        "window.postMessage($message);",
        browser.url,
        0
    )

    //todo: temp
    if (message.contains("SET_CODE_CONTEXT")) {
        println("*********************************")
        println("*********************************")
        println("*********************************")
        println(message)
    }

}


fun <T> jsonToObject(jsonStr: String, type: Class<T>): T {
    return CommonObjectMapper.objectMapper.readValue(jsonStr, type)
}
