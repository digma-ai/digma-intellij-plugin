package org.digma.intellij.plugin.ui.insights

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.insights.InsightsServiceImpl
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.insights.model.SetInsightDataListMessage
import org.digma.intellij.plugin.ui.jcef.JCefComponent
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


    fun refreshInsightsList(backendQueryParams: MutableMap<String, Any>) {

        val insightsResponse = try {
            AnalyticsService.getInstance(project).getInsights(backendQueryParams)
        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, project, e, "Error loading insights {}", e.message)
            "{\"totalCount\":0,\"insights\":[]}"
        }

        val msg = SetInsightDataListMessage(insightsResponse)
        jCefComponent?.let {
            serializeAndExecuteWindowPostMessageJavaScript(it.jbCefBrowser.cefBrowser, msg)
        }
    }
}
