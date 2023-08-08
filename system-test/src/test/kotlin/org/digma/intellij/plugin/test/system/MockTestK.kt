package org.digma.intellij.plugin.test.system

import com.intellij.openapi.diagnostic.Logger
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.log.Log

class MockTestK : LightJavaCodeInsightFixtureTestCase() {

    private val logger = Logger.getInstance(MockTestK::class.java)

    fun `test that all services are up and running`() {
        Log.test(logger,"Requesting AnalyticsService")
        AnalyticsService.getInstance(project)
    }

}