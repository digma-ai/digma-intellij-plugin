package org.digma.intellij.plugin.test.system

import ai.grazie.nlp.utils.dropWhitespaces
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.refactoring.suggested.startOffset
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import junit.framework.TestCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.digma.intellij.plugin.analytics.AnalyticsProvider
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.RestAnalyticsProvider
import org.digma.intellij.plugin.document.DocumentInfoContainer
import org.digma.intellij.plugin.document.DocumentInfoService
import org.digma.intellij.plugin.idea.psi.java.JavaCodeLensService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.rest.AboutResult
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.model.rest.insights.InsightsOfMethodsRequest
import org.digma.intellij.plugin.model.rest.insights.InsightsOfMethodsResponse
import org.digma.intellij.plugin.model.rest.version.BackendDeploymentType
import org.digma.intellij.plugin.test.system.framework.WaitFinishRule
import org.digma.intellij.plugin.test.system.framework.WaitForAsync
import org.gradle.internal.impldep.org.junit.Rule
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`


class MockTestK : LightJavaCodeInsightFixtureTestCase() {

    @get:Rule
    val waitRule: WaitFinishRule = WaitFinishRule()

    private val logger = Logger.getInstance(MockTestK::class.java)
    private lateinit var analyticsProviderProxyMock: AnalyticsProvider
//    private lateinit var analyticsProvider: RestAnalyticsProvider
    private val analyticsService: AnalyticsService
        get() {
            return AnalyticsService.getInstance(project)
        }

    // mock test data
    private val environmentList = EnvironmentListMock
    private val expectedInsightsOfMethodsResponse: InsightsOfMethodsResponse
        get() {
            return MockInsightsOfMethodsResponseFactory(environmentList[0])
        }

    override fun setUp() {
        super.setUp()
        Log.test(logger, "Starting SetUp")
        Log.test(logger, "Mocking AnalyticsProvider")


        analyticsProviderProxyMock = prepareMock()
//        analyticsProvider = mock

    }

    override fun tearDown() {
        waitRule.waitForCompletion()
        Log.test(logger, "Tearing down")
//
//        runBlocking {
//            delay(3000L)
//        }

        super.tearDown()
    }

    override fun getTestDataPath(): String {
        return "src/test/resources"
    }

    private fun prepareMock(): RestAnalyticsProvider {
        val mock = mock(RestAnalyticsProvider::class.java)
//        val field = analyticsService.javaClass.getDeclaredField("analyticsProvider")
        val proxyField = analyticsService.javaClass.getDeclaredField("analyticsProviderProxy")
//        field.isAccessible = true
        proxyField.isAccessible = true
//        field.set(analyticsService, mock)
        proxyField.set(analyticsService, mock)
        mockGetEnvironments(mock)
        mockGetAbout(mock)
        mockGetInsightsOfMethodsForEnv1(mock)

        return mock
    }


    private fun mockGetInsightsOfMethodsForEnv1(mock: RestAnalyticsProvider) {
        `when`(mock.getInsightsOfMethods(any(InsightsOfMethodsRequest::class.java)))
            .thenAnswer {
                return@thenAnswer expectedInsightsOfMethodsResponse
            }
    }

    private fun mockGetAbout(mock: RestAnalyticsProvider) {
        `when`(mock.getAbout()).thenReturn(AboutResult("1.0.0", BackendDeploymentType.Unknown))
    }

    private fun mockGetEnvironments(mock: RestAnalyticsProvider) {
        `when`(mock.getEnvironments()).thenReturn(environmentList)
    }
    private fun navigateToMethod(editor: Editor, methodLine: Int) {
        editor.caretModel.moveToLogicalPosition(LogicalPosition(if (methodLine.dec() < 0) 0 else methodLine.dec(), 0))
    }

    @WaitForAsync
    fun `test that all services are up and running`() {
        Log.test(logger, "Requesting AnalyticsService")
        val analytics = AnalyticsService.getInstance(project)
        waitRule.signalComplete()
    }

    @WaitForAsync
    fun `test that analytics service is up and running`() {
        Log.test(logger, "Requesting AnalyticsService")
        val analytics = AnalyticsService.getInstance(project)
        TestCase.assertNotNull(analytics)
        waitRule.signalComplete()
    }

    @WaitForAsync
    fun `test get private field of analytics service`() {
        Log.test(logger, "Requesting AnalyticsService")
        val analytics = analyticsService
        TestCase.assertNotNull(analytics)
        val field = analytics.javaClass.getDeclaredField("analyticsProviderProxy")
        field.isAccessible = true
        val analyticsImpl = field.get(analytics)
        TestCase.assertNotNull(analyticsImpl)
        waitRule.signalComplete()
    }


    @WaitForAsync
    fun `test that analytics service returns mocked environment`() {
        val environments = analyticsService.environments
        TestCase.assertEquals(environmentList, environments)
        waitRule.signalComplete()
    }

    @WaitForAsync
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
        waitRule.signalComplete()
    }


    @WaitForAsync
    fun `test move caret to method line`() {
        val file = myFixture.configureByFile("EditorEventsHandler.java")
        myFixture.openFileInEditor(file.virtualFile)
        val editor = myFixture.editor

        //moving caret to line 79 (method selectionChanged) (line number is 79 but the logical line position is 78)
        val newLogicalPosition = LogicalPosition(78, 7)
        editor.caretModel.moveToLogicalPosition(newLogicalPosition)
        val caretPosition = editor.caretModel.currentCaret.offset

        val endOffset = editor.caretModel.visualLineEnd
        val startOffset = editor.caretModel.visualLineStart
        editor.selectionModel.setSelection(startOffset, endOffset)
        val selectedText = editor.selectionModel.selectedText
        Log.test(logger, "Selected text: $selectedText")

        TestCase.assertNotNull(selectedText)
        TestCase.assertEquals("public void selectionChanged(@NotNull FileEditorManagerEvent editorManagerEvent) {\n", selectedText?.dropWhitespaces())

        waitRule.signalComplete()
    }

    @WaitForAsync
    fun `test that Digma infoService opened the same file`() {
        val file = myFixture.configureByFile("EditorEventsHandler.java")
        assertTrue(DocumentInfoService.getInstance(project).focusedFile == file.virtualFile)
        myFixture.openFileInEditor(file.virtualFile)
        assertTrue(DocumentInfoService.getInstance(project).focusedFile == file.virtualFile)

        waitRule.signalComplete()
    }

    @WaitForAsync
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

        waitRule.signalComplete()
    }

    @WaitForAsync
    fun `test that Insights are retrieved from analytics service`() {
        val file = myFixture.configureByFile("EditorEventsHandler.java")
        val expectedMethodInsights = expectedInsightsOfMethodsResponse
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

        insights?.toList()?.forEachIndexed { index, it ->
            TestCase.assertTrue(it.second.isNotEmpty())
            it.also { (methodId, insights) ->
                insights.forEachIndexed { indexed, insight: CodeObjectInsight ->
                    TestCase.assertEquals(methodId, insight.codeObjectId)
                    TestCase.assertEquals(expectedMethodInsights.methodsWithInsights[index].insights[indexed], insights[indexed])
                }
            }
        }

        waitRule.signalComplete()
    }


    @WaitForAsync
    fun `test get method from file and find the range`() {
        val file = myFixture.configureByFile("EditorEventsHandler.java")
        val methodList: MutableList<PsiMethod> = mutableListOf()
        file.accept(object : JavaRecursiveElementVisitor() {
            override fun visitMethod(method: PsiMethod) {
                super.visitMethod(method)
                methodList.add(method)
            }
        })

        val documentInfoService = DocumentInfoService.getInstance(project)

        runBlocking {
            delay(1000L)
        }

        val documentInfoContainer: DocumentInfoContainer? = documentInfoService.getDocumentInfo(file.virtualFile)

        val methodRef = documentInfoContainer?.javaClass?.getDeclaredMethod("getMethodInfos")
        methodRef?.isAccessible = true
        @Suppress("UNCHECKED_CAST") val methodInfos: List<MethodInfo>? = methodRef?.invoke(documentInfoContainer) as List<MethodInfo>?

        methodInfos?.forEach { method: MethodInfo ->
            Log.test(logger, "method: ${method.name}, methodOffsetInfile: ${method.offsetAtFileUri},")
        }

        TestCase.assertNotNull(methodInfos)
        TestCase.assertEquals(methodList.size, methodInfos?.size)

        waitRule.signalComplete()
    }
}

