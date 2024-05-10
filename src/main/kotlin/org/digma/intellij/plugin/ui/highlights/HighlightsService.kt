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
import org.digma.intellij.plugin.model.rest.highlights.HighlightsRequest


@Service(Service.Level.PROJECT)
class HighlightsService(val project: Project) : InsightsServiceImpl(project) {

    private val logger = Logger.getInstance(this::class.java)

    companion object {
        @JvmStatic
        fun getInstance(project: Project): HighlightsService {
            return project.service<HighlightsService>()
        }
    }

    fun getHighlightsImpact(request: HighlightsRequest): String? {
        EDT.assertNonDispatchThread()

        return try {
            val result = AnalyticsService.getInstance(project).getHighlightsImpact(request)
            result
        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, project, e, "Error loading highlights impact {}", e.message)
            null
        }
    }

    fun getHighlightsPerformance(queryParams: MutableMap<String, Any>): String? {
        EDT.assertNonDispatchThread()

        return try {
            val result = AnalyticsService.getInstance(project).getHighlightsPerformance(queryParams)
            result
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

    fun getHighlightsPerformanceV2(request: HighlightsRequest): String? {
        EDT.assertNonDispatchThread()

        return try {
            val result = AnalyticsService.getInstance(project).getHighlightsPerformanceV2(request)
            result
        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, project, e, "Error loading highlights performance {}", e.message)
            null
        }
    }

    fun getHighlightsTopInsightsV2(request: HighlightsRequest): String? {
        EDT.assertNonDispatchThread()

        return try {
            val highlightsPerformance = AnalyticsService.getInstance(project).getHighlightsTopInsightsV2(request)
            highlightsPerformance
        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, project, e, "Error loading highlights top insights {}", e.message)
            null
        }
    }

    fun getHighlightsScaling(request: HighlightsRequest): String? {
        EDT.assertNonDispatchThread()

        return try {
            val highlightsPerformance = AnalyticsService.getInstance(project).getHighlightsScaling(request)
            highlightsPerformance
        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, project, e, "Error loading highlights top insights {}", e.message)
            null
        }
    }
}
