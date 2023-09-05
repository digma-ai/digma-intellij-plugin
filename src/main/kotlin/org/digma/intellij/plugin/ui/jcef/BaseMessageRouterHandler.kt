package org.digma.intellij.plugin.ui.jcef

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefQueryCallback
import org.cef.handler.CefMessageRouterHandlerAdapter
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.stopWatchStart
import org.digma.intellij.plugin.common.stopWatchStop
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.jcef.common.OpenInBrowserRequest
import org.digma.intellij.plugin.model.rest.jcef.common.SendTrackingEventRequest
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import java.util.function.Consumer

abstract class BaseMessageRouterHandler(val project: Project) : CefMessageRouterHandlerAdapter() {

    val logger = Logger.getInstance(this::class.java)


    val objectMapper: ObjectMapper = ObjectMapper()

    init {
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        objectMapper.setDateFormat(StdDateFormat())
    }


    override fun onQuery(
        browser: CefBrowser,
        frame: CefFrame,
        queryId: Long,
        request: String,
        persistent: Boolean,
        callback: CefQueryCallback,
    ): Boolean {

        Backgroundable.executeOnPooledThread {

            try {
                val stopWatch = stopWatchStart()

                val requestJsonNode = objectMapper.readTree(request)
                val action: String = requestJsonNode["action"].asText()

                Log.log(logger::trace, "executing action {}", action)

                //do common messages for all apps, or call doOnQuery
                when (action) {

                    JCefMessagesUtils.GLOBAL_OPEN_TROUBLESHOOTING_GUIDE -> {
                        EDT.ensureEDT {
                            MainToolWindowCardsController.getInstance(project).showTroubleshooting()
                        }
                    }

                    JCefMessagesUtils.GLOBAL_OPEN_URL_IN_DEFAULT_BROWSER -> {
                        val openBrowserRequest = JCefMessagesUtils.parseJsonToObject(request, OpenInBrowserRequest::class.java)
                        openBrowserRequest?.let {
                            it.payload?.url?.let { url ->
                                BrowserUtil.browse(url)
                            }
                        }
                    }

                    JCefMessagesUtils.GLOBAL_SEND_TRACKING_EVENT -> {
                        val trackingRequest = JCefMessagesUtils.parseJsonToObject(request, SendTrackingEventRequest::class.java)
                        trackingRequest?.let {
                            it.payload?.let { pl ->
                                ActivityMonitor.getInstance(project).registerCustomEvent(pl.eventName, pl.data)
                            }
                        }
                    }

                    else -> {
                        doOnQuery(project, browser, requestJsonNode, action)
                    }

                }


                stopWatchStop(stopWatch, Consumer { time: Long ->
                    Log.log(logger::trace, "action {} took {}", action, time)
                })

            } catch (e: Exception) {
                Log.debugWithException(logger, e, "Exception in onQuery {}", request)
                ErrorReporter.getInstance().reportError(project, "BaseMessageRouterHandler.onQuery", e)
            }
        }


        //return success regardless of the background thread
        callback.success("")
        return true
    }

    /**
     * each app router handler should implement this method for specific app messages
     */
    abstract fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, action: String)


    override fun onQueryCanceled(browser: CefBrowser?, frame: CefFrame?, queryId: Long) {
        Log.log(logger::debug, "jcef query canceled")
    }
}