package org.digma.intellij.plugin.test.system

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiFile
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.RestAnalyticsProvider
import org.digma.intellij.plugin.document.DocumentInfoService
import org.digma.intellij.plugin.jcef.common.JCefBrowserUtil
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.test.system.framework.BrowserPushEventCollector
import org.digma.intellij.plugin.test.system.framework.MessageBusTestListeners
import org.digma.intellij.plugin.test.system.framework.mockRestAnalyticsProvider
import org.digma.intellij.plugin.ui.recentactivity.RecentActivityService
import org.digma.intellij.plugin.ui.recentactivity.RecentActivityUpdater
import org.mockito.Mockito


open class DigmaTestCase : LightJavaCodeInsightFixtureTestCase() {

    protected val logger = Logger.getInstance(DigmaTestCase::class.java)

    val systemTestDataPath = "src/test/resources"

    protected lateinit var messageBusTestListeners: MessageBusTestListeners

    protected lateinit var jsonCollector: BrowserPushEventCollector

    val objectMapper: ObjectMapper = JCefBrowserUtil.getObjectMapper()

    protected val analyticsService: AnalyticsService
        get() {
            return AnalyticsService.getInstance(project)
        }

    protected val recentActivityService: RecentActivityService
        get() {
            return project.service<RecentActivityService>()
        }

    protected lateinit var mockAnalyticsProvider: RestAnalyticsProvider

    @Volatile
    protected var readyToAssert: Boolean = false
        private set
    protected val recentActivityUpdater: RecentActivityUpdater
        get() {
            return project.getService(RecentActivityUpdater::class.java)
        }


    protected fun readyToTest() {
        readyToAssert = true
    }

    protected fun notReadyToTest() {
        readyToAssert = false
    }


    override fun setUp() {
        super.setUp()
        mockAnalyticsProvider = mockRestAnalyticsProvider(project)
        messageBusTestListeners = MessageBusTestListeners(project.messageBus)
        jsonCollector = BrowserPushEventCollector()
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

    fun setupExecuteJSOf(spiedCefBrowser: CefBrowser) {
        Mockito.doAnswer {
            it.getArgument(0, String::class.java)
                .also { payload ->
                    val stripedPayload = payload.substringAfter("window.postMessage(").substringBeforeLast(");")
                    val jsonpayload = objectMapper.readTree(stripedPayload)
                    val action = jsonpayload.get("action").asText()
                    val browserJson: JsonNode = jsonpayload.get("payload")
                    jsonCollector.addEvent(action, browserJson)
                }
        }.`when`(spiedCefBrowser).executeJavaScript(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt())
    }


    fun waitForDocumentInfoToLoad(file: PsiFile) {
        runBlocking {
            while (DocumentInfoService.getInstance(project).getDocumentInfo(file.virtualFile) == null) {
                delay(100L)
            }
        }
    }

    fun waitForRecentActivityToLoad() {
        runBlocking {
            while (recentActivityService.getRecentActivities(analyticsService.environment.getEnvironments()) == null) {
                delay(100L)
            }
        }
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    }

    fun waitFor(timeMillis: Long, reason: String) {
        Log.test(logger::info, "wait $timeMillis millis for $reason")
        runBlocking {
            delay(timeMillis)
        }
    }

    fun waitForAndDispatch(timeMillis: Long, reason: String) {
        waitFor(timeMillis, reason)
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    }


    fun assertQueueOfActionWithinTimeout(action: String, timeout: Long, assertionFunction: (JsonNode) -> Unit) {
        runBlocking {
            withTimeoutOrNull(timeout) {
                while (isActive) {
                    if (jsonCollector.hasEvents(action)) {
                        val event = jsonCollector.popEvent(action)
                        Log.test(logger::info, "assertQueueOfActionWithinTimeout found event for action $action")
                        try {
                            assertionFunction(event)
                            break
                        } catch (e: Throwable) {
                            Log.test(logger::info, "assertQueueOfActionWithinTimeout failed for action $action\n", e)
                        }
                    }
                    delay(500L)
                }
            } ?: fail("timeout waiting for $action to pass assertion")


        }
    }
}
