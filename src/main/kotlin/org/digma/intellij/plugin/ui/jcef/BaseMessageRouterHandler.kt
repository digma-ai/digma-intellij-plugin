package org.digma.intellij.plugin.ui.jcef

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.impl.HTMLEditorProvider
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefQueryCallback
import org.cef.handler.CefMessageRouterHandlerAdapter
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.stopWatchStart
import org.digma.intellij.plugin.common.stopWatchStop
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.jcef.common.SendTrackingEventRequest
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import org.digma.intellij.plugin.ui.ToolWindowShower
import org.digma.intellij.plugin.ui.jcef.model.BackendInfoMessage
import org.digma.intellij.plugin.ui.jcef.model.GetFromPersistenceRequest
import org.digma.intellij.plugin.ui.jcef.model.OpenInDefaultBrowserRequest
import org.digma.intellij.plugin.ui.jcef.model.OpenInInternalBrowserRequest
import org.digma.intellij.plugin.ui.jcef.model.SaveToPersistenceRequest
import org.digma.intellij.plugin.ui.jcef.persistence.JCEFPersistenceService

abstract class BaseMessageRouterHandler(val project: Project) : CefMessageRouterHandlerAdapter() {

    val logger = Logger.getInstance(this::class.java)

    val objectMapper: ObjectMapper = createObjectMapper()

    override fun onQuery(
        browser: CefBrowser,
        frame: CefFrame,
        queryId: Long,
        request: String,
        persistent: Boolean,
        callback: CefQueryCallback,
    ): Boolean {

        Log.log(logger::trace, "got onQuery event {}", request)

        Backgroundable.executeOnPooledThread {

            try {
                val stopWatch = stopWatchStart()

                val requestJsonNode = objectMapper.readTree(request)
                val action: String = requestJsonNode["action"].asText()

                Log.log(logger::trace, "executing action {}", action)

                //do common messages for all apps, or call doOnQuery
                when (action) {
                    JCefMessagesUtils.GLOBAL_REGISTER ->{
                        project.service<RegistrationEventHandler>().register(requestJsonNode);
                    }
                    JCefMessagesUtils.GLOBAL_OPEN_TROUBLESHOOTING_GUIDE -> {
                        project.service<ActivityMonitor>()
                            .registerCustomEvent("troubleshooting link clicked", mapOf("origin" to getOriginForTroubleshootingEvent()))
                        EDT.ensureEDT {
                            project.service<ToolWindowShower>().showToolWindow()
                            project.service<MainToolWindowCardsController>().showTroubleshooting()
                        }
                    }

                    JCefMessagesUtils.GLOBAL_OPEN_URL_IN_DEFAULT_BROWSER -> {
                        val openBrowserRequest = JCefMessagesUtils.parseJsonToObject(request, OpenInDefaultBrowserRequest::class.java)
                        openBrowserRequest?.let {
                            it.payload.url.let { url ->
                                BrowserUtil.browse(url)
                            }
                        }
                    }

                    JCefMessagesUtils.GLOBAL_OPEN_URL_IN_EDITOR_TAB -> {
                        val openInInternalBrowserRequest = jsonToObject(request, OpenInInternalBrowserRequest::class.java)
                        EDT.ensureEDT {
                            HTMLEditorProvider.openEditor(
                                project,
                                openInInternalBrowserRequest.payload.title,
                                openInInternalBrowserRequest.payload.url,
                                "<!DOCTYPE html>\n" +
                                        "<html lang=\"en\">\n" +
                                        "  <head>\n" +
                                        "    <meta charset=\"UTF-8\" />\n" +
                                        "    <style>\n" +
                                        "      body {\n" +
                                        "        display: flex;\n" +
                                        "        justify-content: center;\n" +
                                        "        padding-top: 100px;\n" +
                                        "        text-align: center;\n" +
                                        "      }\n" +
                                        "    </style>\n" +
                                        "  </head>\n" +
                                        "  <body>\n" +
                                        "    <h1>Timeout loading page<h1>\n" +
                                        "  </body>\n" +
                                        "</html>"
                            )
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

                    JCefMessagesUtils.GLOBAL_SAVE_TO_PERSISTENCE -> {
                        val saveToPersistenceRequest = jsonToObject(request, SaveToPersistenceRequest::class.java)
                        JCEFPersistenceService.getInstance(project).saveToPersistence(saveToPersistenceRequest)
                    }

                    JCefMessagesUtils.GLOBAL_GET_FROM_PERSISTENCE -> {
                        val getFromPersistenceRequest = jsonToObject(request, GetFromPersistenceRequest::class.java)
                        JCEFPersistenceService.getInstance(project).getFromPersistence(browser, getFromPersistenceRequest)
                    }

                    else -> {
                        doOnQuery(project, browser, requestJsonNode, request, action)
                    }

                }


                stopWatchStop(stopWatch) { time: Long ->
                    Log.log(logger::trace, "action {} took {}", action, time)
                }

            } catch (e: Throwable) {
                Log.debugWithException(logger, e, "Exception in onQuery {}", request)
                ErrorReporter.getInstance().reportError(project, "BaseMessageRouterHandler.onQuery", e)
            }
        }


        //return success regardless of the background thread
        callback.success("")
        return true
    }

    abstract fun getOriginForTroubleshootingEvent(): String

    /**
     * each app router handler should implement this method for specific app messages
     */
    abstract fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String, action: String)


    override fun onQueryCanceled(browser: CefBrowser?, frame: CefFrame?, queryId: Long) {
        Log.log(logger::trace, "jcef query canceled")
    }


    protected fun doCommonInitialize(browser: CefBrowser) {
        val about = AnalyticsService.getInstance(project).about
        val message = BackendInfoMessage(about)
        Log.log(logger::trace, project, "sending {} message", JCefMessagesUtils.GLOBAL_SET_BACKEND_INFO)
        serializeAndExecuteWindowPostMessageJavaScript(browser, message)
    }

}