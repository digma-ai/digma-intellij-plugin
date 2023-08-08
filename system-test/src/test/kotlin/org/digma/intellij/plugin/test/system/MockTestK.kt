package org.digma.intellij.plugin.test.system

import ai.grazie.nlp.utils.dropWhitespaces
import com.intellij.openapi.diagnostic.Logger
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import junit.framework.TestCase
import org.digma.intellij.plugin.analytics.AnalyticsProvider
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.RestAnalyticsProvider
import org.digma.intellij.plugin.idea.psi.java.JavaCodeLensService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.AboutResult
import org.digma.intellij.plugin.model.rest.version.BackendDeploymentType
import org.gradle.internal.impldep.org.junit.Assert
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


        val mock = prepareMock()
        analyticsProviderProxyMock = mock
        analyticsProvider = mock


    }

    override fun getTestDataPath(): String {
        return "src/test/resources"
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

    fun `test mock injection to Analytics service`() {
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

    fun `test open file and move caret`() {
        val file = myFixture.configureByFile("EditorEventsHandler.java")
        myFixture.openFileInEditor(file.virtualFile)
        val editor = myFixture.editor

        val caretPosition = editor.caretModel.currentCaret.offset
        Log.test(logger, "Caret position: $caretPosition")

        val fileText = editor.document.text
//        Log.test(logger, "File text:\n {}", fileText)

        val classKeywordIndex = fileText.indexOf("class")
        if (classKeywordIndex != -1) {
            //mover caret to line 37

            val newCaretPosition = editor.offsetToLogicalPosition(classKeywordIndex)
            editor.caretModel.moveToLogicalPosition(newCaretPosition)

            val lineNumber = editor.document.getLineNumber(classKeywordIndex)
            editor.selectionModel.selectLineAtCaret()

            val selectedText = editor.selectionModel.selectedText
            Log.test(logger, "Selected text: $selectedText")

            TestCase.assertNotNull(selectedText)
            TestCase.assertEquals("public class EditorEventsHandler implements FileEditorManagerListener {\n", selectedText?.dropWhitespaces())

            val javaCodeLens = JavaCodeLensService.getInstance(project)
            val codeLens = javaCodeLens.getCodeLens(file)

            TestCase.assertNotNull(codeLens)
        }
    }


}