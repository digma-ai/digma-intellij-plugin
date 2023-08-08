package org.digma.intellij.plugin.test.system

import com.intellij.openapi.diagnostic.Logger
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import junit.framework.TestCase
import org.digma.intellij.plugin.analytics.AnalyticsProvider
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.RestAnalyticsProvider
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.AboutResult
import org.digma.intellij.plugin.model.rest.version.BackendDeploymentType
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`


class MockTestK : LightJavaCodeInsightFixtureTestCase() {

    private val logger = Logger.getInstance(MockTestK::class.java)
    private lateinit var analyticsProviderProxyMock: AnalyticsProvider
    private lateinit var analyticsProvider: RestAnalyticsProvider
    private val analyticsService: AnalyticsService
        get() {
            return AnalyticsService.getInstance(project)
        }

    private val environmentList = listOf("env1_mock", "env2_mock")

    override fun setUp() {
        super.setUp()
        Log.test(logger, "Starting SetUp")
        Log.test(logger, "Mocking AnalyticsProvider")

//        val analyticsProviderMock = mock(AnalyticsProvider::class.java)
//        val field = analyticsService.javaClass.getDeclaredField("analyticsProviderProxy")
//        field.isAccessible = true
//        field.set(analyticsService, analyticsProviderMock)
//        `when`(analyticsProviderMock.getEnvironments()).thenReturn(environmentList)
        val mock = prepareMock()
        analyticsProviderProxyMock = mock
        analyticsProvider = mock


    }

    private fun prepareMock(): RestAnalyticsProvider {
        val mock = mock(RestAnalyticsProvider::class.java)
        val field = analyticsService.javaClass.getDeclaredField("analyticsProvider")
        val proxyField = analyticsService.javaClass.getDeclaredField("analyticsProviderProxy")
        field.isAccessible = true
        proxyField.isAccessible = true
        field.set(analyticsService, mock)
        proxyField.set(analyticsService, mock)
        `when`(mock.getEnvironments()).thenReturn(environmentList)
        `when`(mock.getAbout()).thenReturn(AboutResult("1.0.0", BackendDeploymentType.Unknown))
        return mock
    }

    fun `test that all services are up and running`() {
        Log.test(logger, "Requesting AnalyticsService")
        val analytics = AnalyticsService.getInstance(project)
    }

    fun `test that analytics service is up and running`() {
        Log.test(logger, "Requesting AnalyticsService")
        val analytics = AnalyticsService.getInstance(project)
        TestCase.assertNotNull(analytics)
    }

    fun `test get private field of analytics service`() {
        Log.test(logger, "Requesting AnalyticsService")
        val analytics = analyticsService
        TestCase.assertNotNull(analytics)
        val field = analytics.javaClass.getDeclaredField("analyticsProviderProxy")
        field.isAccessible = true
        val analyticsImpl = field.get(analytics)
        TestCase.assertNotNull(analyticsImpl)
    }

    fun `test mock injection to Analytics service`(){
        val analyticsProviderMock = prepareMock()
//        `when`(analyticsProviderMock.getEnvironments()).thenReturn(environmentList)
        val field = analyticsService.javaClass.getDeclaredField("analyticsProvider")
        field.isAccessible = true
        val analyticsImpl = field.get(analyticsService)
        TestCase.assertEquals(analyticsProviderMock, analyticsImpl)
        TestCase.assertNotNull(analyticsImpl)

    }

    fun `test that analytics service returns mocked environment`() {
        val environments = analyticsService.environments
        Log.test(logger, "got Environments: $environments")
        TestCase.assertEquals(environmentList, environments)
    }



}