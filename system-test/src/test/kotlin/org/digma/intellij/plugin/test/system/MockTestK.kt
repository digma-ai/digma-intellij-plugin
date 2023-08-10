package org.digma.intellij.plugin.test.system

import ai.grazie.nlp.utils.dropWhitespaces
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import junit.framework.TestCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.digma.intellij.plugin.analytics.AnalyticsProvider
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.RestAnalyticsProvider
import org.digma.intellij.plugin.document.DocumentInfoService
import org.digma.intellij.plugin.idea.psi.java.JavaCodeLensService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.AboutResult
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.model.rest.insights.InsightsOfMethodsRequest
import org.digma.intellij.plugin.model.rest.version.BackendDeploymentType
import org.mockito.Mockito.any
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.notNull
import org.mockito.Mockito.`when`


class MockTestK : LightJavaCodeInsightFixtureTestCase() {

    private val logger = Logger.getInstance(MockTestK::class.java)
    private lateinit var analyticsProviderProxyMock: AnalyticsProvider
    private lateinit var analyticsProvider: RestAnalyticsProvider
    private val analyticsService: AnalyticsService
        get() {
            return AnalyticsService.getInstance(project)
        }

    private val environmentList = EnvironmentListMock

    override fun setUp() {
        super.setUp()
        Log.test(logger, "Starting SetUp")
        Log.test(logger, "Mocking AnalyticsProvider")


        val mock = prepareMock()
        analyticsProviderProxyMock = mock
        analyticsProvider = mock

    }

    override fun tearDown() {
        Log.test(logger, "Tearing down")

        runBlocking {
            delay(2000L)
        }

        super.tearDown()
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
        mockGetEnvironments(mock)
        mockGetAbout(mock)
        mockGetInsightsOfMethodsForEnv1(mock)

//        `when`(mock.execute<String?>(notNull())).thenAnswer {
//            val request = it.arguments[0]
//            Log.test(logger, "In thenAnswer: Requesting $request")
////            Log.test(logger, "stackTrace: ${Thread.currentThread().stackTrace}")
//            return@thenAnswer null
//        }
        return mock
    }

    private fun mockGetInsightsOfMethodsForEnv1(mock: RestAnalyticsProvider) {
        `when`(mock.getInsightsOfMethods(any(InsightsOfMethodsRequest::class.java)))
//            .thenReturn(MockInsightsOfMethodsResponseFactory(environmentList[0]))
            .thenAnswer {
            val request = it.arguments[0] as InsightsOfMethodsRequest
            Log.test(logger, "In thenAnswer: Requesting InsightsOfMethods for $request")
            return@thenAnswer MockInsightsOfMethodsResponseFactory(environmentList[0])
        }
    }

    private fun mockGetAbout(mock: RestAnalyticsProvider) {
        `when`(mock.getAbout()).thenReturn(AboutResult("1.0.0", BackendDeploymentType.Unknown))
    }

    private fun mockGetEnvironments(mock: RestAnalyticsProvider) {
        `when`(mock.getEnvironments()).thenReturn(environmentList)
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


    fun `test that analytics service returns mocked environment`() {
        val environments = analyticsService.environments
        TestCase.assertEquals(environmentList, environments)
    }

    fun `test open file and move caret`() {
        val file: PsiFile = myFixture.configureByFile("EditorEventsHandler.java")
        myFixture.openFileInEditor(file.virtualFile)
        val editor: Editor = myFixture.editor

        val caretPosition = editor.caretModel.currentCaret.offset
        Log.test(logger, "Caret position: $caretPosition")

        val fileText = editor.document.text

        val classKeywordIndex = fileText.indexOf("class")
        if (classKeywordIndex != -1) {
            // move caret to line 37
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

    private fun navigateToMethod(editor: Editor, methodLine: Int) {
        editor.caretModel.moveToLogicalPosition(LogicalPosition(if (methodLine.dec() < 0) 0 else methodLine.dec(), 0))
    }

    fun `move caret to method line`() {
        val file = myFixture.configureByFile("EditorEventsHandler.java")
        myFixture.openFileInEditor(file.virtualFile)
        val editor = myFixture.editor

        //moving caret to line 79 (method selectionChanged) (line number is 79 but the logical line position is 78)
        val newLogicalPosition = LogicalPosition(78, 7)
        editor.caretModel.moveToLogicalPosition(newLogicalPosition)
        val caretPosition = editor.caretModel.currentCaret.offset

        val endOffset = editor.caretModel.visualLineEnd
        val selectedText = editor.selectionModel.selectedText
        Log.test(logger, "Selected text: $selectedText")

        TestCase.assertNotNull(selectedText)
        TestCase.assertEquals("public void selectionChanged(@NotNull FileEditorManagerEvent editorManagerEvent) {\n", selectedText?.dropWhitespaces())
    }

    fun `test that Digma infoService opened the same file`() {
        val file = myFixture.configureByFile("EditorEventsHandler.java")
        assertTrue(DocumentInfoService.getInstance(project).focusedFile == file.virtualFile)
        myFixture.openFileInEditor(file.virtualFile)
        assertTrue(DocumentInfoService.getInstance(project).focusedFile == file.virtualFile)
    }

    fun `test that digma infoService opened the same file after switching to another file`() {
        val file = myFixture.configureByFile("EditorEventsHandler.java")
        myFixture.openFileInEditor(file.virtualFile)

        val documentInfoService = DocumentInfoService.getInstance(project)
        assertTrue(documentInfoService.focusedFile == file.virtualFile)
        val file2 = myFixture.configureByFile("EditorEventsHandler2.java")
        assertTrue(documentInfoService.focusedFile == file2.virtualFile)
        assertTrue(documentInfoService.focusedFile != file.virtualFile)

        myFixture.openFileInEditor(file.virtualFile)
        assertTrue(documentInfoService.focusedFile == file.virtualFile)
    }

    fun `test that Insights are retrieved from analytics service`() {
        val file = myFixture.configureByFile("EditorEventsHandler.java")
        myFixture.openFileInEditor(file.virtualFile)

        val documentInfoService = DocumentInfoService.getInstance(project)
        assertTrue(documentInfoService.focusedFile == file.virtualFile)

        runBlocking {
            delay(1000L)
        }

        val documentInfoContainer = documentInfoService.getDocumentInfo(file.virtualFile)
        TestCase.assertNotNull(documentInfoContainer)

        val insights: MutableMap<String, MutableList<CodeObjectInsight>>? = documentInfoContainer?.allMethodWithInsightsMapForCurrentDocument
        TestCase.assertNotNull("Insights are null", insights)

        insights?.forEach {
            TestCase.assertTrue(it.value.isNotEmpty())
            it.also { (methodId, insights) ->
                insights.forEach { insight ->
                    TestCase.assertEquals(methodId, insight.codeObjectId)
                }
            }
        }
    }
}

