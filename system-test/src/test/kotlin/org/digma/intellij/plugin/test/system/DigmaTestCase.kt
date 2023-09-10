package org.digma.intellij.plugin.test.system

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase4
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.document.DocumentInfoService
import org.digma.intellij.plugin.idea.psi.java.JavaCodeLensService
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

    override fun setUp() {

        super.setUp()
        mockRestAnalyticsProvider(project)
        messageBusTestListeners = MessageBusTestListeners(project.messageBus)
    }

    override fun tearDown() {
        Log.test(logger::info, "tearDown started")
        JavaCodeLensService.getInstance(project).dispose()
        try {
            super.tearDown()
        } catch (ex: Exception) {
            Log.test(logger::warn, "PluginException in tearDown")
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

    fun waitForEvent () {
        val timeMillis: Long = 1000
        Log.test(logger::info, "delay $timeMillis millis for file event")
        runBlocking {
            delay(timeMillis)
        }
    }

}
