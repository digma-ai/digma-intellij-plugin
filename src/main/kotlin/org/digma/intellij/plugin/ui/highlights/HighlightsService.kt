package org.digma.intellij.plugin.ui.highlights

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.insights.InsightsServiceImpl
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.highlights.HighlightsPerformanceResponse


@Service(Service.Level.PROJECT)
class HighlightsService(val project: Project) : InsightsServiceImpl(project) {

    private val logger = Logger.getInstance(this::class.java)

    companion object {
        @JvmStatic
        fun getInstance(project: Project): HighlightsService {
            return project.service<HighlightsService>()
        }
    }

    fun getHighlightsPerformance(queryParams: MutableMap<String, Any>): List<HighlightsPerformanceResponse>? {
        EDT.assertNonDispatchThread()

        return try {
            val highlightsPerformance = AnalyticsService.getInstance(project).getHighlightsPerformance(queryParams)
            highlightsPerformance
        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, project, e, "Error loading highlights performance {}", e.message)
            null
        }
    }

    fun getHighlightsTopInsights(queryParams: MutableMap<String, Any>): String {
        EDT.assertNonDispatchThread()

        return try {
            val highlightsPerformance = AnalyticsService.getInstance(project).getHighlightsTopInsights(queryParams)
            highlightsPerformance
        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, project, e, "Error loading highlights top insights {}", e.message)
            return "{}"
        }
    }
}
