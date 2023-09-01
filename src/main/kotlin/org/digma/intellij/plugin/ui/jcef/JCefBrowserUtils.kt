package org.digma.intellij.plugin.ui.jcef

import org.cef.browser.CefBrowser


fun executeWindowPostMessageJavaScript(browser: CefBrowser, message: String) {

    browser.executeJavaScript(
        "window.postMessage(" + message + ");",
        browser.url,
        0
    )
}