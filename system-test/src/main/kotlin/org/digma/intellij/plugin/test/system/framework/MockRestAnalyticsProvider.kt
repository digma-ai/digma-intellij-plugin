package org.digma.intellij.plugin.test.system.framework

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.RestAnalyticsProvider
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.AboutResult
import org.digma.intellij.plugin.model.rest.insights.InsightsOfMethodsRequest
import org.digma.intellij.plugin.model.rest.insights.InsightsOfMethodsResponse
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityRequest
import org.digma.intellij.plugin.model.rest.version.BackendDeploymentType
import org.digma.intellij.plugin.model.rest.version.PerformanceMetricsResponse
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`


private val logger = Logger.getInstance(Throwable().stackTrace[1].fileName)


val environmentList = listOf(
    "env1_mock",
    "env2_mock",
)
val expectedInsightsOfMethodsResponseEnv1: InsightsOfMethodsResponse
    get() {
        return MockInsightsOfMethodsResponseFactory(environmentList[0])
    }
val expectedInsightsOfMethodsResponseEnv2: InsightsOfMethodsResponse
    get() {
        return MockInsightsOfMethodsResponseFactory(environmentList[1])
    }


var mock: RestAnalyticsProvider? = null

fun mockRestAnalyticsProvider(project: Project) {
    mock = mock(RestAnalyticsProvider::class.java)
    Log.test(logger::info, "Creating RestAnalyticsProvider mock")
    val analyticsService = AnalyticsService.getInstance(project)
    var proxyField = analyticsService.javaClass.getDeclaredField("analyticsProviderProxy")
//    val proxySetter = analyticsService.javaClass.getMethod("setAnalyticsProviderProxy", RestAnalyticsProvider::class.java)
//    proxySetter.isAccessible = true
    
    proxyField.isAccessible = true
    proxyField.set(analyticsService, mock)
    mockGetAbout()
    mockGetEnvironments()
//    mockGetRecentActivity()
    mockGetInsightsOfMethodsForEnv1()
    Log.test(logger::info, "RestAnalyticsProvider mock created")
}

fun mockGetAbout() {
    `when`(mock?.getAbout()).thenAnswer {
        Log.test(logger::info, "mock getAbout")
        AboutResult("1.0.0", BackendDeploymentType.Unknown)
    }
}

private fun mockGetEnvironments() {
    `when`(mock?.getEnvironments()).thenAnswer {
        Log.test(logger::info, "mock getEnvironments - $environmentList")
        return@thenAnswer environmentList
    }
}

private fun mockGetRecentActivity() {
    `when`(mock?.getRecentActivity(RecentActivityRequest(environmentList))).thenAnswer {
        Log.test(logger::info, "mock getRecentActivity")
        return@thenAnswer createRecentActivityResult()
    }
}

private fun mockGetInsightsOfMethodsForEnv1() {
    `when`(mock?.getInsightsOfMethods(any(InsightsOfMethodsRequest::class.java))).thenAnswer {
            Log.test(logger::info, "mock getInsightsOfMethods - ${expectedInsightsOfMethodsResponseEnv1.methodsWithInsights}")
            return@thenAnswer expectedInsightsOfMethodsResponseEnv1
    }
}

private fun mockGetInsightsOfMethodsForEnv2() {
    `when`(mock?.getInsightsOfMethods(any(InsightsOfMethodsRequest::class.java))).thenAnswer {
        Log.test(logger::info, "mock getInsightsOfMethods - ${expectedInsightsOfMethodsResponseEnv2.methodsWithInsights}")
        return@thenAnswer expectedInsightsOfMethodsResponseEnv2
    }
}

private fun mockGetPerformanceMetrics() {
    `when`(mock?.performanceMetrics).thenAnswer {
        Log.test(logger::info, "mock getPerformanceMetrics")
        return@thenAnswer createPerformanceMetricsResult()
    }
}

fun createPerformanceMetricsResult(): PerformanceMetricsResponse {
    return PerformanceMetricsResponse(
        listOf(),
        java.util.Date(),
        java.util.Date()
    )
}
