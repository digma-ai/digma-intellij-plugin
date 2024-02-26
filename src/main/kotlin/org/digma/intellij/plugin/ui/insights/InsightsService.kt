package org.digma.intellij.plugin.ui.insights

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.insights.InsightsServiceImpl
import org.digma.intellij.plugin.ui.insights.model.SetInsightDataListMessage
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.jcef.getQueryMapFromPayload
import org.digma.intellij.plugin.ui.jcef.serializeAndExecuteWindowPostMessageJavaScript


@Service(Service.Level.PROJECT)
class InsightsService(val project: Project) : InsightsServiceImpl(project) {

    private val logger = Logger.getInstance(this::class.java)

    private var jCefComponent: JCefComponent? = null

    companion object {
        @JvmStatic
        fun getInstance(project: Project): InsightsService {
            return project.service<InsightsService>()
        }
    }

    fun setJCefComponent(jCefComponent: JCefComponent?) {
        this.jCefComponent = jCefComponent
    }


    fun refreshInsightsList(jsonNode: JsonNode) {

        val insightsResponse = try {
            val backendQueryParams: Map<String, Any> = getQueryMapFromPayload(jsonNode)
            AnalyticsService.getInstance(project).getInsights(backendQueryParams)
        } catch (_: AnalyticsServiceException) {
            "{\"totalCount\":0,\"insights\":[]}"
        }

        val msg = SetInsightDataListMessage(insightsResponse)
        jCefComponent?.let {
            serializeAndExecuteWindowPostMessageJavaScript(it.jbCefBrowser.cefBrowser, msg)
        }
    }
}
