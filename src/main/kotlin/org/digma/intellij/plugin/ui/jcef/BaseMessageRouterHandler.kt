package org.digma.intellij.plugin.ui.jcef

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.NullNode
import com.intellij.execution.RunManager
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.impl.HTMLEditorProvider
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefQueryCallback
import org.cef.handler.CefMessageRouterHandlerAdapter
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.InsightStatsChangedEvent
import org.digma.intellij.plugin.analytics.getAllEnvironments
import org.digma.intellij.plugin.analytics.getEnvironmentById
import org.digma.intellij.plugin.auth.AuthManager
import org.digma.intellij.plugin.auth.account.DigmaDefaultAccountHolder
import org.digma.intellij.plugin.auth.account.LoginResult
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.createObjectMapper
import org.digma.intellij.plugin.common.stopWatchStart
import org.digma.intellij.plugin.common.stopWatchStop
import org.digma.intellij.plugin.dashboard.DashboardService
import org.digma.intellij.plugin.digmathon.DigmathonService
import org.digma.intellij.plugin.documentation.DocumentationService
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.navigation.CodeLocation
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.scope.ScopeManager
import org.digma.intellij.plugin.scope.SpanScope
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import org.digma.intellij.plugin.ui.ToolWindowShower
import org.digma.intellij.plugin.ui.common.updateObservabilityValue
import org.digma.intellij.plugin.ui.jcef.model.ChangeScopeMessage
import org.digma.intellij.plugin.ui.jcef.model.GetFromPersistenceRequest
import org.digma.intellij.plugin.ui.jcef.model.LoginResultPayload
import org.digma.intellij.plugin.ui.jcef.model.OpenInDefaultBrowserRequest
import org.digma.intellij.plugin.ui.jcef.model.OpenInInternalBrowserRequest
import org.digma.intellij.plugin.ui.jcef.model.SaveToPersistenceRequest
import org.digma.intellij.plugin.ui.jcef.model.SendTrackingEventRequest
import org.digma.intellij.plugin.ui.jcef.model.SetLoginResultMessage
import org.digma.intellij.plugin.ui.jcef.model.SetRegistrationMessage
import org.digma.intellij.plugin.ui.jcef.persistence.JCEFPersistenceService
import org.digma.intellij.plugin.ui.jcef.state.JCEFStateManager

/**
 * BaseMessageRouterHandler is a CommonMessageRouterHandler and also implements CefMessageRouterHandler.
 * it is meant to be used as a base class for CefMessageRouterHandlers and has implementation for common messages.
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

        //Execute the query in a coroutine
        JCefMessageHandlerExecutor.getInstance(project).execute(
            this,
            browser,
            request
        )

        //return success regardless of the background thread
        callback.success("OK")
        return true
    }


    suspend fun onQueryBase(browser: CefBrowser, request: String) {

        try {
            val stopWatch = stopWatchStart()

            val requestJsonNode = objectMapper.readTree(request)
            val action: String = requestJsonNode["action"].asText()

            Log.log(logger::trace, "executing action {}", action)

            //do common messages for all apps, or call doOnQuery
            when (action) {
                JCEFGlobalConstants.GLOBAL_PERSONALIZE_REGISTER -> {
                    val payload = getPayloadFromRequestNonNull(requestJsonNode)
                    val registrationMap: Map<String, String> =
                        payload.fields().asSequence()
                            .associate { mutableEntry: MutableMap.MutableEntry<String, JsonNode> ->
                                Pair(
                                    mutableEntry.key,
                                    mutableEntry.value.asText()
                                )
                            }
                    UserRegistrationManager.getInstance(project).register(registrationMap, JCEFGlobalConstants.GLOBAL_PERSONALIZE_REGISTER)
                }

                JCEFGlobalConstants.GLOBAL_OPEN_TROUBLESHOOTING_GUIDE -> {
                    ActivityMonitor.getInstance(project)
                        .registerUserAction("troubleshooting link clicked", mapOf("origin" to getOriginForTroubleshootingEvent()))
                    withContext(Dispatchers.EDT) {
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
                            if (pl.eventName == "error") {
                                pl.data?.let {
                                    val stackTrace = pl.data["exception.stack-trace"] as String?
                                    val message = pl.data["message"] as String
                                    ErrorReporter.getInstance().reportUIError(project, message, stackTrace, pl.data)
                                }
                            } else {
                                if (pl.data == null) {
                                    ActivityMonitor.getInstance(project).registerCustomEvent(pl.eventName)
                                } else {
                                    ActivityMonitor.getInstance(project).registerCustomEvent(pl.eventName, pl.data)
                                }
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
                    val envId = getEnvironmentIdFromPayload(requestJsonNode)
                    envId?.let { env ->
                        getEnvironmentById(project, env)?.let {
                            DashboardService.getInstance(project).openDashboard("Dashboard Panel - ${it.name} - ${it.type}")
                        }
                    }
                }

                JCEFGlobalConstants.GLOBAL_OPEN_REPORT -> {
                    DashboardService.getInstance(project).openReport("Report")
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
                        EDT.ensureEDT {
                            MainToolWindowCardsController.getInstance(project).showWizard(skipInstallationStep)
                            ToolWindowShower.getInstance(project).showToolWindow()
                        }
                    }
                }

                JCEFGlobalConstants.GLOBAL_CHANGE_SCOPE -> {
                    changeScope(requestJsonNode)
                }

                JCEFGlobalConstants.GLOBAL_GET_ENVIRONMENTS -> {
                    sendEnvironmentsList(browser, getAllEnvironments(project))
                }

                JCEFGlobalConstants.GLOBAL_REGISTER -> {
                    val payload = getPayloadFromRequest(requestJsonNode)
                    payload?.let {
                        val userDetails = mapOf("email" to payload.get("email").asText())
                        ActivityMonitor.getInstance(project).registerCustomEvent("register user", userDetails)

                        val requestParams = getMapFromNode(it, objectMapper)
                        val result = AnalyticsService.getInstance(project).register(requestParams)
                        val message = SetRegistrationMessage(result)
                        serializeAndExecuteWindowPostMessageJavaScript(browser, message)

                        ActivityMonitor.getInstance(project).registerUserAction("user registered", userDetails)
                    }
                }

                JCEFGlobalConstants.GLOBAL_LOGOUT -> {
                    AuthManager.getInstance().logoutAsync("user logout")
                }

                JCEFGlobalConstants.GLOBAL_LOGIN -> {
                    val result = login(requestJsonNode)
                    val message = result?.let {
                        SetLoginResultMessage(LoginResultPayload(result.isSuccess, result.error))
                    } ?: SetLoginResultMessage(LoginResultPayload(false, null))

                    serializeAndExecuteWindowPostMessageJavaScript(browser, message)
                }

                JCEFGlobalConstants.GLOBAL_UPDATE_STATE -> {
                    updateState(requestJsonNode)
                }

                JCEFGlobalConstants.GLOBAL_GET_STATE -> {
                    getState(browser)
                }

                JCEFGlobalConstants.GLOBAL_GET_INSIGHT_STATS -> {
                    val payload = getPayloadFromRequest(requestJsonNode)
                    payload?.let {
                        val scopeNode = payload.get("scope")
                        val insightTypesJsonArray = payload.at("/filters/insights")
                        val servicesJsonArray = payload.at("/filters/services")
                        val insightTypes = mutableListOf<String>()
                        if (insightTypesJsonArray is ArrayNode) {
                            insightTypesJsonArray.forEach { type: JsonNode ->
                                insightTypes.add(type.asText())
                            }
                        }
                        val services = mutableListOf<String>()
                        if (servicesJsonArray is ArrayNode) {
                            servicesJsonArray.forEach { type: JsonNode ->
                                services.add(type.asText())
                            }
                        }
                        if (scopeNode is NullNode) {
                            val stats =
                                AnalyticsService.getInstance(project)
                                    .getInsightsStats(null, insightTypes.joinToString(","), services.joinToString(","))
                            project.messageBus.syncPublisher(InsightStatsChangedEvent.INSIGHT_STATS_CHANGED_TOPIC)
                                .insightStatsChanged(
                                    null,
                                    stats.analyticsInsightsCount,
                                    stats.issuesInsightsCount,
                                    stats.unreadInsightsCount,
                                    stats.criticalInsightsCount,
                                    stats.allIssuesCount
                                )
                        } else {
                            val spanCodeObjectId = scopeNode.get("span").get("spanCodeObjectId").asText()
                            val stats = AnalyticsService.getInstance(project)
                                .getInsightsStats(spanCodeObjectId, insightTypes.joinToString(","), services.joinToString(","))
                            project.messageBus.syncPublisher(InsightStatsChangedEvent.INSIGHT_STATS_CHANGED_TOPIC)
                                .insightStatsChanged(
                                    scopeNode,
                                    stats.analyticsInsightsCount,
                                    stats.issuesInsightsCount,
                                    stats.unreadInsightsCount,
                                    stats.criticalInsightsCount,
                                    stats.allIssuesCount
                                )
                        }
                    }
                }


                JCEFGlobalConstants.GLOBAL_FINISH_DIGMATHON_GAME -> {
                    DigmathonService.getInstance().setFinishDigmathonGameForUser()
                }


                else -> {
                    val handled = doOnQuery(project, browser, requestJsonNode, request, action)
                    if (!handled) {
                        //will be caught below and reported by ErrorReporter
                        throw UnknownActionException("got unknown action $action in ${this.javaClass.name}")
                    }
                }

            }


            stopWatchStop(stopWatch) { time: Long ->
                Log.log(logger::trace, "action {} took {}", action, time)
            }

        } catch (e: Throwable) {
            Log.debugWithException(logger, e, "Exception in onQueryBase {}", request)
            ErrorReporter.getInstance().reportError(project, "BaseMessageRouterHandler.onQueryBase", e)
        }

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


    fun doCommonInitialize(browser: CefBrowser) {

        Log.log(logger::trace, project, "sending {} message", JCEFGlobalConstants.GLOBAL_SET_BACKEND_INFO)
        sendBackendAboutInfo(browser, project)

        sendUserInfoMessage(browser, DigmaDefaultAccountHolder.getInstance().account?.userId, project)

        sendEnvironmentsList(browser, getAllEnvironments(project))

        val insightsStats = AnalyticsService.getInstance(project).getInsightsStats(null, null, null)
        sendScopeChangedMessage(
            browser,
            null,
            CodeLocation(listOf(), listOf()),
            false,
            insightsStats.analyticsInsightsCount,
            insightsStats.issuesInsightsCount,
            insightsStats.unreadInsightsCount,
            null,
            null
        )

        val setRunConfigurationMessageBuilder =
            SetRunConfigurationMessageBuilder(project, browser, RunManager.getInstance(project).selectedConfiguration)
        setRunConfigurationMessageBuilder.sendRunConfigurationAttributes()
    }


    private suspend fun changeScope(requestJsonNode: JsonNode) {
        val payload = getPayloadFromRequest(requestJsonNode)
        payload?.let { pl ->

            val changeScopeMessage = jsonToObject(pl, ChangeScopeMessage::class.java)
            changeScopeMessage.span?.spanCodeObjectId?.let { spanId ->
                val spanScope = SpanScope(spanId)
                ScopeManager.getInstance(project).changeScope(
                    scope = spanScope,
                    scopeContext = changeScopeMessage.context,
                    environmentId = changeScopeMessage.environmentId
                )
            } ?: ScopeManager.getInstance(project).changeToHome(
                openMainPanel = changeScopeMessage.openMainPanel ?: false,
                scopeContext = changeScopeMessage.context,
                environmentId = changeScopeMessage.environmentId
            )
        }
    }


    private fun login(requestJsonNode: JsonNode): LoginResult? {
        val payload = getPayloadFromRequest(requestJsonNode)
        val result = payload?.let {
            try {
                AuthManager.getInstance().logoutSynchronously("user login")
                val result = AuthManager.getInstance().loginSynchronously(it.get("email").asText(), it.get("password").asText(), "user login")
                if (result.isSuccess) {
                    UserRegistrationManager.getInstance(project)
                        .register(mapOf("email" to it.get("email").asText()), JCEFGlobalConstants.GLOBAL_LOGIN)
                }
                result
            } catch (_: Exception) {
                return@let LoginResult(false, null, null)
            }
        }
        return result
    }
}

