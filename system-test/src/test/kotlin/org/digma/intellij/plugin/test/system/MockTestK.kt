package org.digma.intellij.plugin.test.system

import com.intellij.openapi.diagnostic.Logger
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.digma.intellij.plugin.analytics.AnalyticsService

class MockTestK : BasePlatformTestCase() {

    private val logger = Logger.getInstance(MockTestK::class.java)

    fun `test that all services are up and running`() {
        logger.warn("Requesting AnalyticsService")
        AnalyticsService.getInstance(project)
    }

}