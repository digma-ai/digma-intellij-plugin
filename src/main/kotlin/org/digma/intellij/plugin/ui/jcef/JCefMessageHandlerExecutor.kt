package org.digma.intellij.plugin.ui.jcef

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import org.cef.browser.CefBrowser
import org.cef.callback.CefQueryCallback
import org.digma.intellij.plugin.kotlin.ext.launchWithErrorReporting

@Service(Service.Level.PROJECT)
class JCefMessageHandlerExecutor(private val project: Project, private val cs: CoroutineScope) {

    private val logger = Logger.getInstance(this::class.java)

    companion object {
        @JvmStatic
        fun getInstance(project: Project): JCefMessageHandlerExecutor {
            return project.service<JCefMessageHandlerExecutor>()
        }
    }


    fun execute(handler: BaseMessageRouterHandler, browser: CefBrowser, request: String) {
        cs.launchWithErrorReporting("JCefMessageHandlerExecutor.execute", logger) {
            handler.onQueryBase(browser, request)
        }
    }


}