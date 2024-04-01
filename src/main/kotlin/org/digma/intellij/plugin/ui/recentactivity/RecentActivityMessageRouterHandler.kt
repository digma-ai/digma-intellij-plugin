package org.digma.intellij.plugin.ui.recentactivity

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.analytics.getAllEnvironments
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.posthog.MonitoredPanel
import org.digma.intellij.plugin.ui.common.traceButtonName
import org.digma.intellij.plugin.ui.jcef.BaseMessageRouterHandler
import org.digma.intellij.plugin.ui.jcef.getMapFromNode
import org.digma.intellij.plugin.ui.jcef.jsonToObject
import org.digma.intellij.plugin.ui.recentactivity.model.CloseLiveViewMessage
import org.digma.intellij.plugin.ui.recentactivity.model.RecentActivityGoToSpanRequest
import org.digma.intellij.plugin.ui.recentactivity.model.RecentActivityGoToTraceRequest

class RecentActivityMessageRouterHandler(project: Project) : BaseMessageRouterHandler(project) {


    override fun getOriginForTroubleshootingEvent(): String {
        return "recent activity"
    }


    override fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String, action: String): Boolean {


        //exceptions are handles in BaseMessageRouterHandler.onQuery

        when (action) {

            "RECENT_ACTIVITY/INITIALIZE" -> {
                try {
                    doCommonInitialize(browser)
                    val environments = getAllEnvironments(project)
                    project.service<RecentActivityUpdater>().updateLatestActivities(environments)
                } finally {
                    //if there is an exception it will be handled by BaseMessageRouterHandler.
                    // but at least set these two variables.
                    //in any case an exception here means something is very wrong and we should check errors in logs.
                    project.service<LiveViewUpdater>().appInitialized()
                    project.service<RecentActivityService>().appInitialized()
                }
            }

            "RECENT_ACTIVITY/GO_TO_SPAN" -> {
                val recentActivityGoToSpanRequest = jsonToObject(rawRequest, RecentActivityGoToSpanRequest::class.java)
                project.service<RecentActivityService>().processRecentActivityGoToSpanRequest(recentActivityGoToSpanRequest.payload)
            }

            "RECENT_ACTIVITY/GO_TO_TRACE" -> {
                ActivityMonitor.getInstance(project).registerButtonClicked(MonitoredPanel.RecentActivity, traceButtonName)
                val recentActivityGoToTraceRequest = jsonToObject(rawRequest, RecentActivityGoToTraceRequest::class.java)
                project.service<RecentActivityService>().processRecentActivityGoToTraceRequest(recentActivityGoToTraceRequest.payload)
            }

            "RECENT_ACTIVITY/CLOSE_LIVE_VIEW" -> {
                try {
                    val closeLiveViewMessage = jsonToObject(rawRequest, CloseLiveViewMessage::class.java)
                    project.service<RecentActivityService>().liveViewClosed(closeLiveViewMessage)
                } catch (e: Exception) {
                    //we can't miss the close message because then the live view will stay open.
                    // close the live view even if there is an error parsing the message.
                    Log.debugWithException(logger, project, e, "Exception while parsing CloseLiveViewMessage {}", e.message)
                    project.service<RecentActivityService>().liveViewClosed(null)
                }
            }

            "RECENT_ACTIVITY/ADD_ENVIRONMENT" -> {
                val environment = objectMapper.readTree(requestJsonNode.get("payload").toString()).get("environment").asText()
                ActivityMonitor.getInstance(project).registerAddEnvironment(environment)
                environment?.let {
                    service<AddEnvironmentsService>().addEnvironment(it)
                    Backgroundable.executeOnPooledThread {
                        project.service<RecentActivityUpdater>().updateLatestActivities()
                    }
                }
            }


            "RECENT_ACTIVITY/FINISH_ORG_DIGMA_SETUP" -> {
                val environment = objectMapper.readTree(requestJsonNode.get("payload").toString()).get("environment").asText()
                project.service<ActivityMonitor>().registerUserActionEvent("finish environment setup", mapOf("environment" to environment))
                if (environment != null) {
                    service<AddEnvironmentsService>().setEnvironmentSetupFinished(project, environment)
                    Backgroundable.executeOnPooledThread {
                        project.service<RecentActivityUpdater>().updateLatestActivities()
                    }
                }
            }

//            "RECENT_ACTIVITY/CHECK_REMOTE_ENVIRONMENT_CONNECTION" -> {
//                try {
//                    val environment = objectMapper.readTree(requestJsonNode.get("payload").toString()).get("environment").asText()
//                    val serverUrl = objectMapper.readTree(requestJsonNode.get("payload").toString()).get("serverAddress").asText()
//                    val token = objectMapper.readTree(requestJsonNode.get("payload").toString()).get("token").asText()
//                    project.service<ActivityMonitor>()
//                        .registerUserActionEvent("check connection", mapOf("environment" to environment, "serverUrl" to serverUrl))
//                    if (environment != null && serverUrl != null) {
//                        service<AddEnvironmentsService>().setEnvironmentServerUrl(project, environment, serverUrl, token)
//                        Backgroundable.executeOnPooledThread {
//                            val connectionTestResult = project.service<AnalyticsService>().testRemoteConnection(serverUrl, token)
//                            sendRemoteConnectionCheckResult(browser, connectionTestResult)
//                        }
//                    }
//                } catch (e: Exception) {
//                    ErrorReporter.getInstance().reportError(project, "RecentActivityMessageRouterHandler.CHECK_REMOTE_ENVIRONMENT_CONNECTION", e)
//                    sendRemoteConnectionCheckResult(browser, ConnectionTestResult.failure(ExceptionUtils.getNonEmptyMessage(e)))
//                }
//            }

            "RECENT_ACTIVITY/DELETE_ENVIRONMENT" -> {
                val environment = objectMapper.readTree(requestJsonNode.get("payload").toString()).get("environment").asText()
                project.service<ActivityMonitor>().registerUserActionEvent("delete environment", mapOf("environment" to environment))
                environment?.let {
                    Backgroundable.executeOnPooledThread {
                        if (service<AddEnvironmentsService>().isPendingEnv(environment)) {
                            service<AddEnvironmentsService>().removeEnvironment(it)
                        } else {
                            project.service<RecentActivityService>().deleteEnvironment(environment)
                        }
                        project.service<RecentActivityUpdater>().updateLatestActivities()
                    }
                }
            }

            "RECENT_ACTIVITY/ADD_ENVIRONMENT_TO_RUN_CONFIG" -> {
                val environmentId = getEnvironmentIdFromPayload(requestJsonNode)
                environmentId?.let {
                    ActivityMonitor.getInstance(project)
                        .registerUserActionEvent("add environment to run config", mapOf("environment" to environmentId))
                    service<AddEnvironmentsService>().addToCurrentRunConfig(project, it)
                    project.service<RecentActivityUpdater>().updateLatestActivities()
                }
            }

            "RECENT_ACTIVITY/CREATE_ENVIRONMENT" -> {
                val request: MutableMap<String, Any> = getMapFromNode(requestJsonNode.get("payload"), objectMapper)
                project.service<RecentActivityService>().createEnvironment(request)
                project.service<RecentActivityUpdater>().updateLatestActivities()
            }

            "RECENT_ACTIVITY/DELETE_ENVIRONMENT_V2" -> {
                val environmentId = getEnvironmentIdFromPayload(requestJsonNode)
                environmentId?.let {
                    project.service<RecentActivityService>().deleteEnvironmentV2(environmentId)
                    project.service<RecentActivityUpdater>().updateLatestActivities()
                }
            }

            else -> return false

        }

        return true
    }

}