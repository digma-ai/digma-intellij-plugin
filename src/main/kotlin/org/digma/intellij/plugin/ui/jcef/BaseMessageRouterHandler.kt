package org.digma.intellij.plugin.ui.jcef

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode
import com.intellij.ide.BrowserUtil
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
import org.digma.intellij.plugin.common.createObjectMapper
import org.digma.intellij.plugin.common.stopWatchStart
import org.digma.intellij.plugin.common.stopWatchStop
import org.digma.intellij.plugin.dashboard.DashboardService
import org.digma.intellij.plugin.documentation.DocumentationService
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.insights.InsightsStatsResult
import org.digma.intellij.plugin.model.rest.navigation.CodeLocation
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.scope.ScopeManager
import org.digma.intellij.plugin.scope.SpanScope
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import org.digma.intellij.plugin.ui.ToolWindowShower
import org.digma.intellij.plugin.ui.common.updateObservabilityValue
import org.digma.intellij.plugin.ui.jcef.model.BackendInfoMessage
import org.digma.intellij.plugin.ui.jcef.model.GetFromPersistenceRequest
import org.digma.intellij.plugin.ui.jcef.model.OpenInDefaultBrowserRequest
import org.digma.intellij.plugin.ui.jcef.model.OpenInInternalBrowserRequest
import org.digma.intellij.plugin.ui.jcef.model.SaveToPersistenceRequest
import org.digma.intellij.plugin.ui.jcef.model.SendTrackingEventRequest
import org.digma.intellij.plugin.ui.jcef.persistence.JCEFPersistenceService
import org.digma.intellij.plugin.ui.jcef.state.JCEFStateManager

/**
 * BaseMessageRouterHandler is a CommonMessageRouterHandler and also implements CefMessageRouterHandler.
 * it is meant to be used as base class for CefMessageRouterHandlers and has implementation for common messages.
 */
abstract class BaseMessageRouterHandler(protected val project: Project) : CommonMessageRouterHandler, CefMessageRouterHandlerAdapter() {

    protected val logger = Logger.getInstance(this::class.java)

    override val objectMapper: ObjectMapper = createObjectMapper()

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
                    JCEFGlobalConstants.GLOBAL_REGISTER -> {
                        val payload = getPayloadFromRequestNonNull(requestJsonNode)
                        val registrationMap: Map<String, String> =
                            payload.fields().asSequence()
                                .associate { mutableEntry: MutableMap.MutableEntry<String, JsonNode> ->
                                    Pair(
                                        mutableEntry.key,
                                        mutableEntry.value.asText()
                                    )
                                }
                        UserRegistrationManager.getInstance(project).register(registrationMap)
                    }

                    JCEFGlobalConstants.GLOBAL_OPEN_TROUBLESHOOTING_GUIDE -> {
                        ActivityMonitor.getInstance(project)
                            .registerCustomEvent("troubleshooting link clicked", mapOf("origin" to getOriginForTroubleshootingEvent()))
                        EDT.ensureEDT {
                            ToolWindowShower.getInstance(project).showToolWindow()
                            MainToolWindowCardsController.getInstance(project).showTroubleshooting()
                        }
                    }

                    JCEFGlobalConstants.GLOBAL_OPEN_URL_IN_DEFAULT_BROWSER -> {
                        val openBrowserRequest = jsonToObject(request, OpenInDefaultBrowserRequest::class.java)
                        openBrowserRequest.let {
                            it.payload.url.let { url ->
                                BrowserUtil.browse(url)
                            }
                        }
                    }

                    JCEFGlobalConstants.GLOBAL_OPEN_URL_IN_EDITOR_TAB -> {
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

                    JCEFGlobalConstants.GLOBAL_SEND_TRACKING_EVENT -> {
                        val trackingRequest = jsonToObject(request, SendTrackingEventRequest::class.java)
                        trackingRequest.let {
                            it.payload?.let { pl ->
                                if (pl.data == null) {
                                    ActivityMonitor.getInstance(project).registerCustomEvent(pl.eventName)
                                } else {
                                    ActivityMonitor.getInstance(project).registerCustomEvent(pl.eventName, pl.data)
                                }
                            }
                        }
                    }

                    JCEFGlobalConstants.GLOBAL_SAVE_TO_PERSISTENCE -> {
                        val saveToPersistenceRequest = jsonToObject(request, SaveToPersistenceRequest::class.java)
                        JCEFPersistenceService.getInstance(project).saveToPersistence(saveToPersistenceRequest)
                    }

                    JCEFGlobalConstants.GLOBAL_GET_FROM_PERSISTENCE -> {
                        val getFromPersistenceRequest = jsonToObject(request, GetFromPersistenceRequest::class.java)
                        JCEFPersistenceService.getInstance(project).getFromPersistence(browser, getFromPersistenceRequest)
                    }

                    JCEFGlobalConstants.GLOBAL_OPEN_DASHBOARD -> {
                        val environment = getEnvironmentFromPayload(requestJsonNode)
                        environment?.let { env ->
                            DashboardService.getInstance(project).openDashboard("Dashboard Panel - ${env.name}")
                        }
                    }

                    JCEFGlobalConstants.GLOBAL_OPEN_DOCUMENTATION -> {
                        val payload = getPayloadFromRequest(requestJsonNode)
                        payload?.takeIf { payload.get("page") != null }?.let { pl ->
                            val page = pl.get("page").asText()
                            DocumentationService.getInstance(project).openDocumentation(page)
                        }
                    }

                    JCEFGlobalConstants.GLOBAL_SET_OBSERVABILITY -> {
                        val payload = getPayloadFromRequest(requestJsonNode)
                        payload?.let {
                            val isEnabledObservability = it.get("isObservabilityEnabled").asBoolean()
                            Log.log(logger::trace, "updateSetObservability(Boolean) called")
                            updateObservabilityValue(project, isEnabledObservability)
                        }
                    }

                    JCEFGlobalConstants.GLOBAL_OPEN_INSTALLATION_WIZARD -> {
                        val payload = getPayloadFromRequest(requestJsonNode)

                        payload?.let {
                            val skipInstallationStep = it.get("skipInstallationStep").asBoolean()
                            ActivityMonitor.getInstance(project).registerCustomEvent("show-installation-wizard")
                            EDT.ensureEDT {
                                MainToolWindowCardsController.getInstance(project).showWizard(skipInstallationStep)
                                ToolWindowShower.getInstance(project).showToolWindow()
                            }
                        }

                    }

                    JCEFGlobalConstants.GLOBAL_CHANGE_SCOPE -> {
                        changeScope(requestJsonNode)
                    }


                    JCEFGlobalConstants.GLOBAL_UPDATE_STATE -> {
                        updateState(requestJsonNode)
                    }

                    JCEFGlobalConstants.GLOBAL_GET_STATE -> {
                        getState(browser)
                    }


                    else -> {
                        val handled = doOnQuery(project, browser, requestJsonNode, request, action)
                        if (!handled) {
                            //will be caught bellow and reported by ErrorReporter
                            throw UnknownActionException("got unknown action $action")
                        }
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

    private fun getState(browser: CefBrowser) {
        val state = JCEFStateManager.getInstance(project).getState()
        sendJcefStateMessage(browser, state)
    }

    private fun updateState(requestJsonNode: JsonNode) {
        val payload = getPayloadFromRequest(requestJsonNode)
        payload?.let {
            JCEFStateManager.getInstance(project).updateState(it)
        }
    }

    abstract fun getOriginForTroubleshootingEvent(): String


    override fun onQueryCanceled(browser: CefBrowser?, frame: CefFrame?, queryId: Long) {
        Log.log(logger::trace, "jcef query canceled")
    }


    protected fun doCommonInitialize(browser: CefBrowser) {
        try {
            val about = AnalyticsService.getInstance(project).about
            val message = BackendInfoMessage(about)
            Log.log(logger::trace, project, "sending {} message", JCEFGlobalConstants.GLOBAL_SET_BACKEND_INFO)
            serializeAndExecuteWindowPostMessageJavaScript(browser, message)
        } catch (e: Exception) {
            Log.debugWithException(logger, project, e, "error calling about")
        }

        val insightsStats = AnalyticsService.getInstance(project).getInsightsStats(null)

        updateDigmaEngineStatus(project, browser)

        sendEnvironmentsList(browser, AnalyticsService.getInstance(project).environment.getEnvironments())

        sendScopeChangedMessage(
            browser,
            null,
            CodeLocation(listOf(), listOf()),
            false,
            insightsStats?.analyticsInsightsCount ?: 0,
            insightsStats?.issuesInsightsCount ?: 0
        )
    }

    fun changeScope(requestJsonNode: JsonNode) {
        val payload = getPayloadFromRequest(requestJsonNode)
        payload?.let { pl ->
            val span = pl.get("span")
            val spanScope: SpanScope? = span?.takeIf { span !is NullNode }?.let { sp ->
                val spanObj = objectMapper.readTree(sp.toString())
                val spanId = if (spanObj.get("spanCodeObjectId") is NullNode) null else spanObj.get("spanCodeObjectId").asText()
                spanId?.let {
                    SpanScope(it, null, null, null)
                }
            }

            spanScope?.let {
                ScopeManager.getInstance(project).changeScope(it)
            } ?: ScopeManager.getInstance(project).changeToHome()

        }
    }

}

