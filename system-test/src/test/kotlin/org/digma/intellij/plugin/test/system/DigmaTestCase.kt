package org.digma.intellij.plugin.test.system

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiFile
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.RestAnalyticsProvider
import org.digma.intellij.plugin.document.DocumentInfoService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.test.system.framework.MessageBusTestListeners
import org.digma.intellij.plugin.test.system.framework.mockRestAnalyticsProvider


open class DigmaTestCase : LightJavaCodeInsightFixtureTestCase() {

    protected val logger = Logger.getInstance(DigmaTestCase::class.java)

    val systemTestDataPath = "src/test/resources"

    protected lateinit var messageBusTestListeners: MessageBusTestListeners

    protected val analyticsService: AnalyticsService
        get() {
            return AnalyticsService.getInstance(project)
        }

    protected lateinit var mockAnalyticsProvider: RestAnalyticsProvider

    @Volatile
    protected var readyToAssert: Boolean = false
        private set


    protected fun readyToAssert() {
        readyToAssert = true
    }

    protected fun notReadyToAssert() {
        readyToAssert = false
    }


    override fun setUp() {
        super.setUp()
        mockAnalyticsProvider = mockRestAnalyticsProvider(project)
        messageBusTestListeners = MessageBusTestListeners(project.messageBus)
        readyToAssert = false
    }

    override fun tearDown() {
        Log.test(logger::info, "tearDown started")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        try {
            super.tearDown()
        } catch (ex: Exception) {
            Log.test(logger::warn, "Exception in tearDown")
        }
        Log.test(logger::info, "tearDown completed")
    }

    override fun getTestDataPath(): String {
        return systemTestDataPath
    }

    fun waitForDocumentInfoToLoad(file: PsiFile) {
        runBlocking {
            while (DocumentInfoService.getInstance(project).getDocumentInfo(file.virtualFile) == null) {
                delay(100L)
            }
        }
    }

    fun waitFor(timeMillis: Long, reason: String) {
        Log.test(logger::info, "wait $timeMillis millis for $reason")
        runBlocking {
            delay(timeMillis)
        }
    }

}
