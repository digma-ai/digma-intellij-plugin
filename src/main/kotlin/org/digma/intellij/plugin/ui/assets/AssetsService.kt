package org.digma.intellij.plugin.ui.assets

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.analytics.NoSelectedEnvironmentException
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.insights.InsightsViewOrchestrator
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.posthog.MonitoredPanel
import org.digma.intellij.plugin.scope.ScopeManager
import org.digma.intellij.plugin.scope.SpanScope

@Service(Service.Level.PROJECT)
class AssetsService(private val project: Project) : Disposable {

    private val logger = Logger.getInstance(this::class.java)

    companion object {
        @JvmStatic
        fun getInstance(project: Project): AssetsService {
            return project.service<AssetsService>()
        }
    }


    fun getAssetCategories(queryParams: Map<String, Any?>): String {
        EDT.assertNonDispatchThread()

        try {
            Log.log(logger::trace, project, "got get categories request")
            val categories = AnalyticsService.getInstance(project).getAssetCategories(queryParams)
            AnalyticsService.getInstance(project).checkInsightExists()
            Log.log(logger::trace, project, "got categories [{}]", categories)
            return categories
        } catch (e: NoSelectedEnvironmentException) {
            Log.log(logger::debug, project, "no environment when calling getCategories {}", e.message)
            return "{ \"assetCategories\": [] }"
        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, project, e, "Error loading categories {}", e.message)
            return "{ \"assetCategories\": [] }"
        }
    }


    fun getServices(): String? {
        try {
            Log.log(logger::trace, project, "got get assets request")
            val services = AnalyticsService.getInstance(project).services
            Log.log(logger::trace, project, "got services")
            return services
        } catch (e: NoSelectedEnvironmentException) {
            Log.log(logger::debug, project, "no environment when calling getAssets {}", e.message)
            return null
        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, project, e, "Error getting services {}", e.message)
            return null
        }
    }


    fun getAssetFilters(queryParams: Map<String, Any>): String {
        EDT.assertNonDispatchThread()

        try {
            Log.log(logger::trace, project, "got get asset filters request")
            val assetFilters = AnalyticsService.getInstance(project).getAssetFilters(queryParams)
            AnalyticsService.getInstance(project).checkInsightExists()
            Log.log(logger::trace, project, "got asset filters [{}]", assetFilters)
            return assetFilters
        } catch (e: NoSelectedEnvironmentException) {
            Log.log(logger::trace, project, "no environment when calling getAssetFilters {}", e.message)
            return ""
        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, project, e, "Error loading asset filters {}", e.message)
            return ""
        }
    }


    fun getAssets(queryParams: Map<String, Any>): String {
        EDT.assertNonDispatchThread()

        try {
            Log.log(logger::trace, project, "got get assets request")
            val assets = AnalyticsService.getInstance(project).getAssets(queryParams)
            Log.log(logger::trace, project, "got assets [{}]", assets)
            return assets
        } catch (e: NoSelectedEnvironmentException) {
            Log.log(logger::trace, project, "no environment when calling getAssets {}", e.message)
            return ""
        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, project, e, "Error loading assets {}", e.message)
            return ""
        }
    }


    fun showAsset(spanId: String) {
        EDT.assertNonDispatchThread()

        Log.log(logger::trace, project, "showAsset called for {}", spanId)
        ActivityMonitor.getInstance(project).registerSpanLinkClicked(MonitoredPanel.Assets)
        project.getService(InsightsViewOrchestrator::class.java).showInsightsForCodelessSpan(spanId)

        ScopeManager.getInstance(project).changeScope(SpanScope(spanId, ""))
    }

    override fun dispose() {
        //nothing to do, used as parent disposable
    }

}