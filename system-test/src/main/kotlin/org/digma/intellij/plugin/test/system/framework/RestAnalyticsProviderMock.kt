package org.digma.intellij.plugin.test.system.framework

import com.intellij.openapi.diagnostic.LogLevel
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.RestAnalyticsProvider
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.AboutResult
import org.digma.intellij.plugin.model.rest.insights.InsightsOfMethodsRequest
import org.digma.intellij.plugin.model.rest.insights.InsightsOfMethodsResponse
import org.digma.intellij.plugin.model.rest.version.BackendDeploymentType
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`


private val logger = Logger.getInstance(Throwable().stackTrace[1].fileName)


val environmentList = listOf(
    "env1_mock",
    "env2_mock",
)
val expectedInsightsOfMethodsResponse: InsightsOfMethodsResponse
    get() {
        return MockInsightsOfMethodsResponseFactory(environmentList[0])
    }

val mock: RestAnalyticsProvider = mock(RestAnalyticsProvider::class.java)

fun mockRestAnalyticsProvider(project: Project) {
    val analyticsService = AnalyticsService.getInstance(project)
    var proxyField = analyticsService.javaClass.getDeclaredField("analyticsProviderProxy")
    proxyField.isAccessible = true
    proxyField.set(analyticsService, mock)
    mockGetAbout()
    mockGetEnvironments()
    mockGetInsightsOfMethodsForEnv1()
    Log.test(logger, "RestAnalyticsProvider mock created.")
}

fun mockGetAbout() {
    `when`(mock.getAbout()).thenReturn(AboutResult("1.0.0", BackendDeploymentType.Unknown))
}

private fun mockGetEnvironments() {
    `when`(mock.getEnvironments()).thenReturn(environmentList)
}

private fun mockGetInsightsOfMethodsForEnv1() {
    `when`(mock.getInsightsOfMethods(any(InsightsOfMethodsRequest::class.java)))
        .thenAnswer {
            return@thenAnswer expectedInsightsOfMethodsResponse
        }
}
