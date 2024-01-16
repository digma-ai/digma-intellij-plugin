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
import org.digma.intellij.plugin.editor.CurrentContextUpdater
import org.digma.intellij.plugin.jcef.common.JCefBrowserUtil
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret
import org.digma.intellij.plugin.test.system.framework.BrowserPushEventCollector
import org.digma.intellij.plugin.test.system.framework.MessageBusTestListeners
import org.digma.intellij.plugin.test.system.framework.mockRestAnalyticsProvider
import org.digma.intellij.plugin.ui.recentactivity.RecentActivityService
import org.digma.intellij.plugin.ui.recentactivity.RecentActivityUpdater
import org.mockito.Mockito


abstract class DigmaTestCase : LightJavaCodeInsightFixtureTestCase() {

    protected val logger = Logger.getInstance(DigmaTestCase::class.java)

    val systemTestDataPath = "src/test/resources"

    protected lateinit var messageBusTestListeners: MessageBusTestListeners

    private var jsonCollector: BrowserPushEventCollector = BrowserPushEventCollector()

    val objectMapper: ObjectMapper = JCefBrowserUtil.getObjectMapper()

    /**
     * retrieves the AnalyticsService from the project
     * @return Analytics service
     */
    protected val analyticsService: AnalyticsService
        get() {
            return AnalyticsService.getInstance(project)
        }

    /**
     * retrieves the DocumentInfoService from the project
     * @return RecentActivity service
     */
    protected val recentActivityService: RecentActivityService
        get() {
            return project.service<RecentActivityService>()
        }

    /**
     * cached field for the mock analytics provider to re mock methods.
     */
    protected lateinit var mockAnalyticsProvider: RestAnalyticsProvider


    protected val recentActivityUpdater: RecentActivityUpdater
        get() {
            return project.getService(RecentActivityUpdater::class.java)
        }


    override fun setUp() {
        super.setUp()
        mockAnalyticsProvider = mockRestAnalyticsProvider(project)
        messageBusTestListeners = MessageBusTestListeners(project.messageBus)
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

    abstract fun getTestProjectFileNames(): Array<String>;

    fun createDigmaCodeObjectId(className: String, methodName: String): String {
        return "$className\$_\$$methodName"
    }

    /**
     * Given a spied CefBrowser, Replace the original behavior to executeJavaScript method on the Spied CefBrowser to collect the json payload sent to
     * the browser based on the action that was sent to the browser.
     */
    fun replaceExecuteJavaScriptOf(spiedCefBrowser: CefBrowser) {
        Mockito.doAnswer {
            it.getArgument(0, String::class.java)
                .also { payload ->
                    val stripedPayload = payload.substringAfter("window.postMessage(").substringBeforeLast(");")
                    val jsonPayload = objectMapper.readTree(stripedPayload)
                    val action = jsonPayload.get("action").asText()
                    val browserJson: JsonNode = jsonPayload.get("payload")
                    jsonCollector.addEvent(action, browserJson)
                }
        }.`when`(spiedCefBrowser).executeJavaScript(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt())
    }


    /**
     * Helper method to wait for the DocumentInfoService to load the document info for the given file.
     *
     * @param file - the PsiFile to wait for to load into the DocumentInfoService.
     */
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

    /**
     * Helper method to wait for the given time in millis.
     * Note that it will block the EDT thread. The purpose of this method is to wait for the background tasks to complete.
     *
     * @param timeMillis - time to wait in millis
     * @param reason - reason for waiting
     */
    fun waitFor(timeMillis: Long, reason: String) {
        Log.test(logger::info, "wait $timeMillis millis for $reason")
        runBlocking {
            delay(timeMillis)
        }
    }

    /**
     * Helper method to wait for the given time in millis and dispatch all events in the EDT queue.
     * After waitFor is completed, this method will dispatch all events in the EDT queue.
     *
     * @param timeMillis - time to wait in millis
     * @param reason - reason for waiting
     */
    fun waitForAndDispatch(timeMillis: Long, reason: String) {
        waitFor(timeMillis, reason)
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    }


    /**
     * method to assert that a given action and json payload was sent to the browser, 
     * and that the payload passes the given assertion function within the given timeout.
     * Note: this method blocks the test execution until the assertion passes or the timeout is reached.
     * Note: the assertion will fail only if no payload with the action has passed the assertion within the timeout.
     * @param action - action name
     * @param timeout - timeout in millis
     * @param assertionFunction - function to assert the json payload
     */
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
    
    fun getMethodUnderCaret(): MethodUnderCaret? {
        var contextUpdater = project.getService(CurrentContextUpdater::class.java)
        var methodUnderCaretField = contextUpdater.javaClass.getDeclaredField("latestMethodUnderCaret")
        methodUnderCaretField.isAccessible = true
        return methodUnderCaretField.get(contextUpdater) as MethodUnderCaret?
    }
}
