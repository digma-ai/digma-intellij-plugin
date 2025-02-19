package org.digma.intellij.plugin.ui.insights

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.activation.UserActivationService
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.common.createObjectMapper
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.insights.InsightsServiceImpl
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.insights.MarkInsightsAsReadScope
import org.digma.intellij.plugin.model.rest.insights.issues.GetIssuesRequestPayload
import org.digma.intellij.plugin.ui.insights.model.SetAllInsightsAsReadData
import org.digma.intellij.plugin.ui.insights.model.SetAllInsightsMarkAsReadMessage
import org.digma.intellij.plugin.ui.insights.model.SetDismissedData
import org.digma.intellij.plugin.ui.insights.model.SetDismissedMessage
import org.digma.intellij.plugin.ui.insights.model.SetInsightDataListMessage
import org.digma.intellij.plugin.ui.insights.model.SetInsightsAsReadData
import org.digma.intellij.plugin.ui.insights.model.SetInsightsData
import org.digma.intellij.plugin.ui.insights.model.SetInsightsMarkAsReadMessage
import org.digma.intellij.plugin.ui.insights.model.SetIssuesData
import org.digma.intellij.plugin.ui.insights.model.SetIssuesDataListMessage
import org.digma.intellij.plugin.ui.insights.model.SetIssuesFilterMessage
import org.digma.intellij.plugin.ui.insights.model.SetUnDismissedData
import org.digma.intellij.plugin.ui.insights.model.SetUnDismissedMessage
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.jcef.model.ErrorPayload
import org.digma.intellij.plugin.ui.jcef.serializeAndExecuteWindowPostMessageJavaScript


@Service(Service.Level.PROJECT)
class InsightsService(val project: Project) : InsightsServiceImpl(project) {

    private val logger = Logger.getInstance(this::class.java)

    private var jCefComponent: JCefComponent? = null

    private val objectMapper = createObjectMapper()

    companion object {
        @JvmStatic
        fun getInstance(project: Project): InsightsService {
            return project.service<InsightsService>()
        }
    }

    override fun dispose(){
        jCefComponent = null
    }

    fun setJCefComponent(jCefComponent: JCefComponent?) {
        this.jCefComponent = jCefComponent
    }

    fun refreshInsightsList(backendQueryParams: MutableMap<String, Any>) {
        val viewMode = backendQueryParams["insightViewType"] as String;
        val message = try {
            val insights = AnalyticsService.getInstance(project).getInsights(backendQueryParams)
            onInsightReceived(insights)
            SetInsightDataListMessage(SetInsightsData(insights, viewMode))
        } catch (e: AnalyticsServiceException) {
            Log.debugWithException(logger, project, e, "Error loading insights {}", e.message)
            val error = ErrorPayload(e.nonNullMessage)
            SetInsightDataListMessage(SetInsightsData("{\"totalCount\":0,\"insights\":[]}", viewMode), error)
        }

        jCefComponent?.let {
            serializeAndExecuteWindowPostMessageJavaScript(it.jbCefBrowser.cefBrowser, message)
        }
    }

    fun refreshIssuesList(request: GetIssuesRequestPayload) {
       val message = try {
            val issues = AnalyticsService.getInstance(project).getIssues(request)
            reportFirstIssue(issues)
            SetIssuesDataListMessage(SetIssuesData(issues))
        } catch (e: AnalyticsServiceException) {
            Log.debugWithException(logger, project, e, "Error loading issues {}", e.message)
            val error = ErrorPayload(e.nonNullMessage)
            SetIssuesDataListMessage(SetIssuesData("{\"totalCount\":0,\"insights\":[]}"), error)
        }

        jCefComponent?.let {
            serializeAndExecuteWindowPostMessageJavaScript(it.jbCefBrowser.cefBrowser, message)
        }
    }

    private fun reportFirstIssue(issues: String) {

        if (UserActivationService.getInstance().isFirstIssueReceived()) {
            UserActivationService.getInstance().issuesReceivedInProject(project)
            return
        }

        val count = try {
            objectMapper.readTree(issues).get("totalCount").asInt()
        } catch (e: Throwable) {
            0
        }
        if (count > 0) {
            UserActivationService.getInstance().setFirstIssueReceived(project)
        }
    }




    fun refreshIssuesFilters(backendQueryParams: MutableMap<String, Any>) {
        val message = try {
            val filters = AnalyticsService.getInstance(project).getIssuesFilters(backendQueryParams)
            SetIssuesFilterMessage(filters)
        } catch (e: AnalyticsServiceException) {
            Log.debugWithException(logger, project, e, "Error loading issues filters {}", e.message)
        }

        jCefComponent?.let {
            serializeAndExecuteWindowPostMessageJavaScript(it.jbCefBrowser.cefBrowser, message)
        }
    }


    fun undismissInsight(insightId: String) {
        var status = ""
        var error: String? = null
        try {
            AnalyticsService.getInstance(project).undismissInsight(insightId)
            status = "success"
        } catch (e: AnalyticsServiceException) {
            status = "failure"
            error = "Error undismissing  insight";
            Log.warnWithException(logger, project, e, error, e.message)
        }

        val msg = SetUnDismissedMessage(SetUnDismissedData(insightId, status, error))
        jCefComponent?.let {
            serializeAndExecuteWindowPostMessageJavaScript(it.jbCefBrowser.cefBrowser, msg)
        }
    }

    fun markAllInsightsAsRead(scope: JsonNode?, markAsReadScope: MarkInsightsAsReadScope?) {
        var status = ""
        var error: String? = null
        try {
            AnalyticsService.getInstance(project).markAllInsightsAsRead(markAsReadScope)
            status = "success"
        } catch (e: AnalyticsServiceException) {
            status = "failure"
            error = "Error mark as read insight";
            Log.warnWithException(logger, project, e, error, e.message)
        }

        val msg = SetAllInsightsMarkAsReadMessage(SetAllInsightsAsReadData(scope, status, error))
        jCefComponent?.let {
            serializeAndExecuteWindowPostMessageJavaScript(it.jbCefBrowser.cefBrowser, msg)
        }
    }

    fun markInsightsAsRead(insightIds: List<String>) {
        var status = ""
        var error: String? = null
        try {
            AnalyticsService.getInstance(project).markInsightsAsRead(insightIds)
            status = "success"
        } catch (e: AnalyticsServiceException) {
            status = "failure"
            error = "Error mark as read insight";
            Log.warnWithException(logger, project, e, error, e.message)
        }

        val msg = SetInsightsMarkAsReadMessage(SetInsightsAsReadData(insightIds, status, error))
        jCefComponent?.let {
            serializeAndExecuteWindowPostMessageJavaScript(it.jbCefBrowser.cefBrowser, msg)
        }
    }

    fun dismissInsight(insightId: String) {
        var status = ""
        var error: String? = null
        try {
            AnalyticsService.getInstance(project).dismissInsight(insightId)
            status = "success"
        } catch (e: AnalyticsServiceException) {
            status = "failure"
            error = "Error dismissing  insight";
            Log.warnWithException(logger, project, e, error, e.message)
        }

        val msg = SetDismissedMessage(SetDismissedData(insightId, status, error))
        jCefComponent?.let {
            serializeAndExecuteWindowPostMessageJavaScript(it.jbCefBrowser.cefBrowser, msg)
        }
    }


    private fun onInsightReceived(insights: String) {

        if (UserActivationService.getInstance().isFirstInsightReceived()) {
            UserActivationService.getInstance().insightsReceivedInProject(project)
            return
        }

        try {
            val jsonNode = objectMapper.readTree(insights)
            val totalCount = jsonNode.get("totalCount")
            if (totalCount.isInt && totalCount.asInt() > 0) {
                UserActivationService.getInstance().setFirstInsightReceived(project)
            }
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "InsightsService.onInsightReceived", e)
        }
    }

}
