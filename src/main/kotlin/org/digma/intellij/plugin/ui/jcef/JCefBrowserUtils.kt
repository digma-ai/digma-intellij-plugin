package org.digma.intellij.plugin.ui.jcef

import com.fasterxml.jackson.databind.ObjectMapper
import org.cef.browser.CefBrowser


object CommonObjectMapper {
    var objectMapper = ObjectMapper()

    init {
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        objectMapper.setDateFormat(com.fasterxml.jackson.databind.util.StdDateFormat())
    }
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