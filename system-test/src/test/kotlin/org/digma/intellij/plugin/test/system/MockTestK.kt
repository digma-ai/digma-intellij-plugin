package org.digma.intellij.plugin.test.system

import ai.grazie.nlp.utils.dropWhitespaces
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import junit.framework.TestCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.document.DocumentInfoContainer
import org.digma.intellij.plugin.document.DocumentInfoService
import org.digma.intellij.plugin.idea.psi.java.JavaCodeLensService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.navigation.HomeSwitcherService
import org.digma.intellij.plugin.test.system.framework.WaitFinishRule
import org.digma.intellij.plugin.test.system.framework.WaitForAsync
import org.digma.intellij.plugin.test.system.framework.environmentList
import org.digma.intellij.plugin.test.system.framework.expectedInsightsOfMethodsResponseEnv1
import org.digma.intellij.plugin.test.system.framework.mockRestAnalyticsProvider
import org.gradle.internal.impldep.org.junit.Rule
import java.awt.event.MouseEvent
import java.lang.reflect.Field
import kotlin.test.assertNotEquals


class MockTestK : LightJavaCodeInsightFixtureTestCase() {

    private val logger = Logger.getInstance(MockTestK::class.java)

    @get:Rule
    val done: WaitFinishRule = WaitFinishRule()

    private lateinit var messageBusTestListeners: MessageBusTestListeners

    private val analyticsService: AnalyticsService
        get() {
            return AnalyticsService.getInstance(project)
        }

    override fun setUp() {
        super.setUp()
        done.success = false
        mockRestAnalyticsProvider(project)
        messageBusTestListeners = MessageBusTestListeners(project.messageBus)
    }

    override fun tearDown() {
        Log.test(logger::info, "FFS: tearDown")
        while (true) {
            if (done.success) {
                done()
                break
            } else {
                done.waitForCompletion()
            }
        }

        try {
            super.tearDown()
        } catch (ex: Exception) {
            Log.test(logger::info, "FFS: PluginException in tearDown")
        }
    }

    override fun getTestDataPath(): String {
        return "src/test/resources"
    }

    @WaitForAsync
    fun `test that all services are up and running`() {
        Log.test(logger::info, "Requesting analytics service")
        val analytics = AnalyticsService.getInstance(project)
        TestCase.assertNotNull(analytics)
        done()
    }

    @WaitForAsync
    fun `test get private field of analytics service`() {
        Log.test(logger::info, "Requesting AnalyticsService")
        val analytics = analyticsService
        TestCase.assertNotNull(analytics)
        val field = analytics.javaClass.getDeclaredField("analyticsProviderProxy")
        field.isAccessible = true
        val analyticsImpl = field.get(analytics)
        TestCase.assertNotNull(analyticsImpl)
        done()
    }


    @WaitForAsync
    fun `test that analytics service returns mocked environment`() {
        val environments = analyticsService.environments
        TestCase.assertEquals(environmentList, environments)
        done()
    }

    @WaitForAsync
    fun `test open file and move caret`() {
        val file: PsiFile = myFixture.configureByFile("EditorEventsHandler.java")
        myFixture.openFileInEditor(file.virtualFile)
        val editor: Editor = myFixture.editor


        waitForDocumentInfoToLoad(file)

        val caretPosition = editor.caretModel.currentCaret.offset
        Log.test(logger::info, "Caret position: $caretPosition")

        val fileText = editor.document.text

        val classKeywordIndex = fileText.indexOf("class")
        if (classKeywordIndex != -1) {
            // move caret to line 37
            val newCaretPosition = editor.offsetToLogicalPosition(classKeywordIndex)
            editor.caretModel.moveToLogicalPosition(newCaretPosition)

            editor.selectionModel.selectLineAtCaret()

            val selectedText = editor.selectionModel.selectedText
            Log.test(logger::info, "Selected text: $selectedText")

            TestCase.assertNotNull(selectedText)
            TestCase.assertEquals("public class EditorEventsHandler implements FileEditorManagerListener {\n", selectedText?.dropWhitespaces())

            done()
        }
    }

    private fun waitForDocumentInfoToLoad(file: PsiFile) {
        runBlocking {
            while (DocumentInfoService.getInstance(project).getDocumentInfo(file.virtualFile) == null) {
                delay(100L)
            }
        }
    }

    fun `test code vision loaded from insights and click code vision`() {

        val file: PsiFile = myFixture.configureByFile("EditorEventsHandler.java")
        myFixture.openFileInEditor(file.virtualFile)

        val switcherService: HomeSwitcherService = project.getService(HomeSwitcherService::class.java)

        waitForDocumentInfoToLoad(file)

        val javaCodeLens = JavaCodeLensService.getInstance(project)
        val codeLens = javaCodeLens.getCodeLens(file)

        val size = codeLens.size
        Log.test(logger::info, "CodeLens size: $size")
        codeLens.first().also {
            when (val codeVision = it.second) {
                is ClickableTextCodeVisionEntry -> {
                    val augmentedOnClick: (MouseEvent?, Editor) -> Unit = { mouseEvent, editor ->
                        // maybe we should mock the entire clickHandler
                        codeVision.onClick(mouseEvent, editor)
                    }

                    val onClickField: Field = codeVision.javaClass.getDeclaredField("onClick")
                    onClickField.isAccessible = true
                    onClickField.set(codeVision, augmentedOnClick)
                    codeVision.onClick(myFixture.editor)
                }

                else -> Log.test(logger::info, "CodeVision is not ClickableTextCodeVisionEntry")
            }
        }
        done()
    }


    @WaitForAsync
    fun `test move caret to method line`() {
        val file = myFixture.configureByFile("EditorEventsHandler.java")
        myFixture.openFileInEditor(file.virtualFile)
        val editor = myFixture.editor

        //moving caret to line 79 (method selectionChanged) (line number is 79 but the logical line position is 78)
        val newLogicalPosition = LogicalPosition(78, 7)
        editor.caretModel.moveToLogicalPosition(newLogicalPosition)

        val endOffset = editor.caretModel.visualLineEnd
        val startOffset = editor.caretModel.visualLineStart
        editor.selectionModel.setSelection(startOffset, endOffset)
        val selectedText = editor.selectionModel.selectedText
        Log.test(logger::info, "Selected text: $selectedText")

        TestCase.assertNotNull(selectedText)
        TestCase.assertEquals("public void selectionChanged(@NotNull FileEditorManagerEvent editorManagerEvent) {\n", selectedText?.dropWhitespaces())

        done()
    }

    @WaitForAsync
    fun `test that Digma infoService opened the same file`() {
        val file = myFixture.configureByFile("EditorEventsHandler.java")
        assertTrue(DocumentInfoService.getInstance(project).focusedFile == file.virtualFile)
        myFixture.openFileInEditor(file.virtualFile)
        assertTrue(DocumentInfoService.getInstance(project).focusedFile == file.virtualFile)

        done()
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

        done()
    }

    @WaitForAsync
    fun `test that Insights are retrieved from analytics service`() {

        val environments = analyticsService.environments
        TestCase.assertEquals(environmentList, environments)

        val file = myFixture.configureByFile("EditorEventsHandler.java")
        val expectedMethodInsights = expectedInsightsOfMethodsResponseEnv1
        myFixture.openFileInEditor(file.virtualFile)

        val documentInfoService = DocumentInfoService.getInstance(project)
        assertTrue(documentInfoService.focusedFile == file.virtualFile)

        var notified = false
        messageBusTestListeners.registerSubToDocumentInfoChangedEvent {
            Log.test(logger::info, "Got notification")
            notified = true
        }

        while (!notified)
            runBlocking {
                delay(100L)
            }

        val documentInfoContainer = documentInfoService.getDocumentInfo(file.virtualFile)
        TestCase.assertNotNull(documentInfoContainer)

        val insights: MutableMap<String, MutableList<CodeObjectInsight>>? = documentInfoContainer?.allMethodWithInsightsMapForCurrentDocument
        TestCase.assertNotNull("Insights are null", insights)
        TestCase.assertTrue(insights?.size == 4)

        insights?.toList()?.forEachIndexed { index, it ->
            TestCase.assertTrue(it.second.isNotEmpty())
            it.also { (methodId, insights) ->
                insights.forEachIndexed { indexed, insight: CodeObjectInsight ->
                    TestCase.assertEquals(methodId, insight.codeObjectId)
                    TestCase.assertEquals(expectedMethodInsights.methodsWithInsights[index].insights[indexed], insights[indexed])
                }
            }
        }

        done()
    }


    @WaitForAsync
    fun `test get method from file and find the range`() {

        val file = myFixture.configureByFile("EditorEventsHandler.java")
        myFixture.openFileInEditor(file.virtualFile)

        val methodList: MutableList<PsiMethod> = mutableListOf()
        file.accept(object : JavaRecursiveElementVisitor() {
            override fun visitMethod(method: PsiMethod) {
                super.visitMethod(method)
                methodList.add(method)
            }
        })

        runBlocking {
            delay(1000L)
        }

        val documentInfoService = DocumentInfoService.getInstance(project)
        val documentInfoContainer: DocumentInfoContainer? = documentInfoService.getDocumentInfo(file.virtualFile)

        val methodRef = documentInfoContainer?.javaClass?.getDeclaredMethod("getMethodInfos")
        methodRef?.isAccessible = true
        @Suppress("UNCHECKED_CAST") val methodInfos: List<MethodInfo>? = methodRef?.invoke(documentInfoContainer) as List<MethodInfo>?

        methodInfos?.forEach { method: MethodInfo ->
            Log.test(logger::info, "method: ${method.name}, methodOffsetInfile: ${method.offsetAtFileUri},")
        }

        TestCase.assertNotNull(methodInfos)
        TestCase.assertEquals(methodList.size, methodInfos?.size)

        done()
    }

    @WaitForAsync
    fun `test subscribe to documentInfoChange and openFile`() {

        var file: PsiFile? = null
        var assertFinished = false
        var isTheSameName = false
        //prepare
        messageBusTestListeners.registerSubToDocumentInfoChangedEvent {
            Log.test(logger::info, "Test Subscriber - DocumentInfoChanged: documentInfoChanged")
            assertFinished = true
            isTheSameName = it.name == file?.name
        }
        //act
        file = myFixture.configureByFile("EditorEventsHandler.java")
        myFixture.openFileInEditor(file.virtualFile)

        runBlocking {
            while (!assertFinished) {
                delay(100)
            }
        }
        //done
        TestCase.assertTrue(assertFinished)
        assertTrue("This is intended to be called, remove the line to properly test", isTheSameName)
        done()
    }

    @WaitForAsync
    fun `test getting current environment and switching`() {

        var expected = environmentList[0]
        val file = myFixture.configureByFile("EditorEventsHandler.java")
        //register for environment change event
        messageBusTestListeners.registerSubToEnvironmentChangedEvent { newEnv, toRefresh ->
            Log.test(logger::info, "Test Subscriber - EnvironmentChanged: environmentChanged $newEnv")
            TestCase.assertEquals(expected, newEnv)
            done()
        }
        
        analyticsService.environment.setCurrent(expected)
        
        
        runBlocking { 
            delay(100L)
        }
        //verify that env did change
        var afterSet = analyticsService.environment.getCurrent()
        TestCase.assertEquals(expected, afterSet)

        Log.test(logger::info, "Current environment after set: $afterSet")
        
        //should be the same as after set
        var beforeSet = analyticsService.environment.getCurrent()
        TestCase.assertEquals(expected, beforeSet)
        TestCase.assertEquals(beforeSet, afterSet)


        expected = environmentList[1]
        analyticsService.environment.setCurrent(expected)

        runBlocking {
            delay(100L)
        }
        
        afterSet = analyticsService.environment.getCurrent()
        TestCase.assertEquals(expected, afterSet)
        
        assertNotEquals(beforeSet, afterSet)

        // see that insights are retrieved for the new environment


        // receive code vision of method.
        done()
    }


//    @WaitForAsync
//    fun `test triggering process event methods`() {
//        val recentActivityService = RecentActivityService.getInstance(project)
//        
//        
//        
//        val file = myFixture.configureByFile("EditorEventsHandler.java")
//        
//        runBlocking { 
//            delay(2000L)
//        }
//        
//        
//        
//        done() 
//        
//        
//        
//    }

}

