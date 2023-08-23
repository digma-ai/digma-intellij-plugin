package org.digma.intellij.plugin.test.system.framework

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.thoughtworks.qdox.model.JavaClass
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.RestAnalyticsProvider
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.AboutResult
import org.digma.intellij.plugin.model.rest.insights.Duration
import org.digma.intellij.plugin.model.rest.insights.InsightsOfMethodsRequest
import org.digma.intellij.plugin.model.rest.insights.InsightsOfMethodsResponse
import org.digma.intellij.plugin.model.rest.livedata.DurationData
import org.digma.intellij.plugin.model.rest.livedata.DurationLiveData
import org.digma.intellij.plugin.model.rest.livedata.DurationLiveDataRequest
import org.digma.intellij.plugin.model.rest.livedata.LiveDataRecord
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityRequest
import org.digma.intellij.plugin.model.rest.version.BackendDeploymentType
import org.digma.intellij.plugin.model.rest.version.PerformanceMetricsResponse
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.mockito.Mockito.isA
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.sql.Timestamp
import java.util.Date


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
    mockGetRecentActivity()
    mockGetInsightsOfMethods()
    mockGetPerformanceMetrics()
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
    `when`(mock?.getRecentActivity(isA(RecentActivityRequest::class.java))).thenAnswer {
        Log.test(logger::info, "mock getRecentActivity")
        return@thenAnswer createRecentActivityResult()
    }
}

private fun mockGetInsightsOfMethods() {
    `when`(mock?.getInsightsOfMethods(isA(InsightsOfMethodsRequest::class.java))).thenAnswer {
        val currEnv = (it.arguments[0] as InsightsOfMethodsRequest).environment
        Log.test(logger::info, "mock getInsightsOfMethods - ${expectedInsightsOfMethodsResponseEnv1.methodsWithInsights}")
        return@thenAnswer if (currEnv == environmentList[0]) expectedInsightsOfMethodsResponseEnv1 else expectedInsightsOfMethodsResponseEnv2
    }
}

private fun mockGetDurationLiveData() {
    `when`(mock?.getDurationLiveData(isA(DurationLiveDataRequest::class.java))).thenAnswer {
        Log.test(logger::info, "mock getDurationLiveData")
        val currEnv = (it.arguments[0] as DurationLiveDataRequest).environment
        return@thenAnswer createDurationLiveData(currEnv)
    }
}


private fun mockGetPerformanceMetrics() {
    `when`(mock?.performanceMetrics).thenAnswer {
        Log.test(logger::info, "mock getPerformanceMetrics")
        return@thenAnswer createPerformanceMetricsResult()
    }
}

fun createDurationLiveData(env: String): DurationLiveData {
    val defaultLiveDataRecords = listOf(
        LiveDataRecord(
            duration = Duration(
                value = 25.0,
                unit = "ms",
                raw = 2500
            ),
            dateTime = Timestamp(Date().time).toString()
        ),

        LiveDataRecord(
            duration = Duration(
                value = 30.0,
                unit = "ms",
                raw = 3000
            ),
            dateTime = Timestamp(Date().time).toString()
        )
    )
    val defaultDurationData = DurationData(
        percentiles = listOf(),
        codeObjectId = "",
        displayName = "duration data from $env",
    )
    return DurationLiveData(
        liveDataRecords = defaultLiveDataRecords,
        durationData = defaultDurationData
    )
}

fun createPerformanceMetricsResult(): PerformanceMetricsResponse {
    return PerformanceMetricsResponse(
        metrics = listOf(),
        serverStartTime = Date(),
        probeTime = Date()
    )
}
