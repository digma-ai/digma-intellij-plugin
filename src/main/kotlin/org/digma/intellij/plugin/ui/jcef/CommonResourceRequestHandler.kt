package org.digma.intellij.plugin.ui.jcef

import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefResourceRequestHandlerAdapter
import org.cef.misc.BoolRef
import org.cef.network.CefRequest

class CommonResourceRequestHandler : CefResourceRequestHandlerAdapter() {

    override fun onProtocolExecution(browser: CefBrowser?, frame: CefFrame?, request: CefRequest?, allowOsExecution: BoolRef?) {

        allowOsExecution?.set(true)
    }
}
