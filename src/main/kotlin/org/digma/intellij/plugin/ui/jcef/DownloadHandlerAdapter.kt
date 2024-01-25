package org.digma.intellij.plugin.ui.jcef

import org.cef.browser.CefBrowser
import org.cef.callback.CefBeforeDownloadCallback
import org.cef.callback.CefDownloadItem
import org.cef.callback.CefDownloadItemCallback
import org.cef.handler.CefDownloadHandler

class DownloadHandlerAdapter : CefDownloadHandler {
    override fun onBeforeDownload(browser: CefBrowser?, dowloadItem: CefDownloadItem?, suggestedName: String?, callback: CefBeforeDownloadCallback?) {
        callback?.Continue(suggestedName, true)
    }

    override fun onDownloadUpdated(browser: CefBrowser?, downloadItem: CefDownloadItem?, callback: CefDownloadItemCallback?) {
    }
}