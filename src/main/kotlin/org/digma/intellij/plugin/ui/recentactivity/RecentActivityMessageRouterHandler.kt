package org.digma.intellij.plugin.ui.recentactivity

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.analytics.getAllEnvironments
import org.digma.intellij.plugin.digmathon.DigmathonService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.posthog.UserActionOrigin
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
                ActivityMonitor.getInstance(project).registerUserActionWithOrigin("trace button clicked", UserActionOrigin.RecentActivity)
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

            "RECENT_ACTIVITY/ADD_ENVIRONMENT_TO_RUN_CONFIG" -> {
                val environmentId = getEnvironmentIdFromPayload(requestJsonNode)
                environmentId?.let {
                    ActivityMonitor.getInstance(project)
                        .registerUserAction("add environment to run config", mapOf("environment" to environmentId))
                    project.service<RecentActivityService>().addVarRunToConfig(it)
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

            "RECENT_ACTIVITY/GET_DIGMATHON_PROGRESS_DATA" -> {
                RecentActivityService.getInstance(project).setDigmathonProgressData(DigmathonService.getInstance().viewedInsights)
            }

            else -> return false

        }

        return true
    }

}