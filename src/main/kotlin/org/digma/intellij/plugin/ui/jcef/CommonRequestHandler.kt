package org.digma.intellij.plugin.ui.jcef

import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.handler.CefResourceRequestHandler
import org.cef.misc.BoolRef
import org.cef.network.CefRequest
import java.awt.Desktop
import java.net.URI

class CommonRequestHandler : CefRequestHandlerAdapter() {
    override fun onBeforeBrowse(browser: CefBrowser?, frame: CefFrame?, request: CefRequest?, user_gesture: Boolean, is_redirect: Boolean): Boolean {
        try {
            // Check if the URL starts with "mailto:"
            if (request != null) {
                if (request.url?.startsWith("mailto:") == true) {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().mail(URI(request.url))
                        // Returning true indicates that you've handled this URL
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Returning false allows normal navigation to proceed
        return false
    }

    override fun getResourceRequestHandler(
        browser: CefBrowser?,
        frame: CefFrame?,
        request: CefRequest?,
        isNavigation: Boolean,
        isDownload: Boolean,
        requestInitiator: String?,
        disableDefaultHandling: BoolRef?
    ): CefResourceRequestHandler {
        return CommonResourceRequestHandler()
    }
}
