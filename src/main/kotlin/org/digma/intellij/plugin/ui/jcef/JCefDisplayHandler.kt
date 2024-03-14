package org.digma.intellij.plugin.ui.jcef

import com.intellij.openapi.diagnostic.Logger
import org.cef.CefSettings
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefDisplayHandlerAdapter
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log

class JCefDisplayHandler(private val appName: String) : CefDisplayHandlerAdapter() {

    private val logger = Logger.getInstance(this::class.java)

    override fun onAddressChange(browser: CefBrowser, frame: CefFrame, url: String) {
        log("{}: onAddressChange {}", appName, url)
    }

    override fun onTitleChange(browser: CefBrowser, title: String) {
        log("{}: onTitleChange {}", appName, title)
    }

    override fun onStatusMessage(browser: CefBrowser, value: String) {
        log("{}: onStatusMessage {}", appName, value)
    }

    override fun onConsoleMessage(browser: CefBrowser, level: CefSettings.LogSeverity, message: String, source: String, line: Int): Boolean {
        log("{}: onConsoleMessage {}:{} source[{}]", appName, level, message, source)
        return super.onConsoleMessage(browser, level, message, source, line)
    }

    private fun log(message: String, vararg args: Any) {
        try {
            Log.log(logger::trace, message, *args)
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("JCefDisplayHandler.log", e)
        }
    }
}