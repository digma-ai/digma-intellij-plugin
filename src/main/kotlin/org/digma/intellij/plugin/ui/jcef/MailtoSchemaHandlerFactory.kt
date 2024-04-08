package org.digma.intellij.plugin.ui.jcef

import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefCallback
import org.cef.callback.CefSchemeHandlerFactory
import org.cef.handler.CefResourceHandler
import org.cef.handler.CefResourceHandlerAdapter
import org.cef.misc.IntRef
import org.cef.misc.StringRef
import org.cef.network.CefRequest
import org.cef.network.CefResponse

class MailtoSchemaHandlerFactory : CefSchemeHandlerFactory {

    override fun create(browser: CefBrowser?, frame: CefFrame?, schemeName: String?, request: CefRequest?): CefResourceHandler {
        return MailtoResourceHandler()
    }

    fun getSchema(): String {
        return "mailto"
    }
}


class MailtoResourceHandler : CefResourceHandlerAdapter() {

    override fun processRequest(request: CefRequest?, callback: CefCallback?): Boolean {
        callback?.Continue()
        return true
    }

    override fun getResponseHeaders(response: CefResponse?, responseLength: IntRef?, redirectUrl: StringRef?) {
        super.getResponseHeaders(response, responseLength, redirectUrl)
    }

    override fun readResponse(dataOut: ByteArray?, bytesToRead: Int, bytesRead: IntRef?, callback: CefCallback?): Boolean {
        return super.readResponse(dataOut, bytesToRead, bytesRead, callback)
    }

    override fun cancel() {
        super.cancel()
    }
}