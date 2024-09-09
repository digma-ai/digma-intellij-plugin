package org.digma.intellij.plugin.ui.assets

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.activation.UserActivationService
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.createObjectMapper
import org.digma.intellij.plugin.log.Log

@Service(Service.Level.PROJECT)
class AssetsService(private val project: Project) : Disposable {

    private val logger = Logger.getInstance(this::class.java)

    private val objectMapper = createObjectMapper()

    companion object {
        @JvmStatic
        fun getInstance(project: Project): AssetsService {
            return project.service<AssetsService>()
        }
    }


    fun getAssetCategories(queryParams: Map<String, Any?>): String {
        EDT.assertNonDispatchThread()
        Log.log(logger::trace, project, "got get categories request")
        val categories = AnalyticsService.getInstance(project).getAssetCategories(queryParams)
        checkInsightExists()
        Log.log(logger::trace, project, "got categories [{}]", categories)
        return categories
    }


    fun getAssetFilters(queryParams: MutableMap<String, Any>): String {
        EDT.assertNonDispatchThread()

        return try {
            Log.log(logger::trace, project, "got get asset filters request")
            val assetFilters = AnalyticsService.getInstance(project).getAssetFilters(queryParams)
            checkInsightExists()
            Log.log(logger::trace, project, "got asset filters [{}]", assetFilters)
            assetFilters
        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, project, e, "Error loading asset filters {}", e.message)
            ""
        }
    }


    fun getAssets(queryParams: MutableMap<String, Any>): String {
        EDT.assertNonDispatchThread()
        Log.log(logger::trace, project, "got get assets request")
        val assets = AnalyticsService.getInstance(project).getAssets(queryParams)
        Log.log(logger::trace, project, "got assets [{}]", assets)
        return assets
    }


    private fun checkInsightExists() {

        if (UserActivationService.getInstance().isFirstAssetsReceived()) {
            UserActivationService.getInstance().assetsReceivedInProject(project)
            return
        }

        try {
            val insightsExists = AnalyticsService.getInstance(project).insightsExist
            val payload = objectMapper.readTree(insightsExists)
            if (!payload.isMissingNode && payload["insightExists"].asBoolean()) {
                UserActivationService.getInstance().setFirstAssetsReceived(project)
            }
        } catch (e: Throwable) {
            Log.warnWithException(logger, project, e, "error reporting FirstTimeAssetsReceived {}", e)
        }
    }


    override fun dispose() {
        //nothing to do, used as parent disposable
    }

}