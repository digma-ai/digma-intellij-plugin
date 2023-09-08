package org.digma.intellij.plugin.ui.recentactivity

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.posthog.MonitoredPanel
import org.digma.intellij.plugin.ui.jcef.BaseMessageRouterHandler
import org.digma.intellij.plugin.ui.list.insights.traceButtonName
import org.digma.intellij.plugin.ui.recentactivity.model.CloseLiveViewMessage
import org.digma.intellij.plugin.ui.recentactivity.model.RecentActivityGoToSpanRequest
import org.digma.intellij.plugin.ui.recentactivity.model.RecentActivityGoToTraceRequest

class RecentActivityMessageRouterHandler(project: Project) : BaseMessageRouterHandler(project) {


    override fun getOriginForTroubleshootingEvent(): String {
        return "recent activity"
    }


    override fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String, action: String) {


        //exceptions are handles in BaseMessageRouterHandler.onQuery

        when (action) {

            "RECENT_ACTIVITY/INITIALIZE" -> {
                val environments = project.service<AnalyticsService>().environment.getEnvironments()
                project.service<RecentActivityUpdater>().updateLatestActivities(environments)
            }

            "RECENT_ACTIVITY/GO_TO_SPAN" -> {
                val recentActivityGoToSpanRequest = JCefMessagesUtils.parseJsonToObject(rawRequest, RecentActivityGoToSpanRequest::class.java)
                project.service<RecentActivityService>().processRecentActivityGoToSpanRequest(recentActivityGoToSpanRequest.payload)
            }

            "RECENT_ACTIVITY/GO_TO_TRACE" -> {
                project.service<ActivityMonitor>().registerButtonClicked(MonitoredPanel.RecentActivity, traceButtonName)
                val recentActivityGoToTraceRequest = JCefMessagesUtils.parseJsonToObject(rawRequest, RecentActivityGoToTraceRequest::class.java)
                project.service<RecentActivityService>().processRecentActivityGoToTraceRequest(recentActivityGoToTraceRequest.payload)
            }

            "RECENT_ACTIVITY/CLOSE_LIVE_VIEW" -> {
                try {
                    val closeLiveViewMessage = JCefMessagesUtils.parseJsonToObject(rawRequest, CloseLiveViewMessage::class.java)
                    project.service<RecentActivityService>().liveViewClosed(closeLiveViewMessage)
                } catch (e: Exception) {
                    //we can't miss the close message because then the live view will stay open.
                    // close the live view even if there is an error parsing the message.
                    Log.debugWithException(logger, project, e, "Exception while parsing CloseLiveViewMessage {}", e.message)
                    project.service<RecentActivityService>().liveViewClosed(null)
                }
            }

            "RECENT_ACTIVITY/ADD_ENVIRONMENT" -> {
                project.service<ActivityMonitor>().registerCustomEvent("add environment", mapOf())
                project.service<ActivityMonitor>().registerUserAction("add environment")
                val environment = objectMapper.readTree(requestJsonNode.get("payload").toString()).get("environment").asText()
                environment?.let {
                    project.service<AddEnvironmentsService>().addEnvironment(it)
                    project.service<RecentActivityUpdater>().updateLatestActivities()
                }
            }

            "RECENT_ACTIVITY/DELETE_ENVIRONMENT" -> {
                project.service<ActivityMonitor>().registerCustomEvent("delete environment", mapOf())
                project.service<ActivityMonitor>().registerUserAction("delete environment")
                val environment = objectMapper.readTree(requestJsonNode.get("payload").toString()).get("environment").asText()
                environment?.let {
                    project.service<AddEnvironmentsService>().removeEnvironment(it)
                    project.service<RecentActivityUpdater>().updateLatestActivities()
                }
            }

            "RECENT_ACTIVITY/ADD_ENVIRONMENT_TO_RUN_CONFIG" -> {
                project.service<ActivityMonitor>().registerCustomEvent("add environment to run config", mapOf())
                project.service<ActivityMonitor>().registerUserAction("add environment to run config")
                val environment = objectMapper.readTree(requestJsonNode.get("payload").toString()).get("environment").asText()
                environment?.let {
                    project.service<AddEnvironmentsService>().addToCurrentRunConfig(it)
                    project.service<RecentActivityUpdater>().updateLatestActivities()
                }
            }
        }
    }

}