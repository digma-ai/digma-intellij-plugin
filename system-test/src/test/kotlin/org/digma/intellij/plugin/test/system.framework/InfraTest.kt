package org.digma.intellij.plugin.test.system.framework

import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.PlatformTestUtil
import junit.framework.TestCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.digma.intellij.plugin.document.DocumentInfoContainer
import org.digma.intellij.plugin.document.DocumentInfoService
import org.digma.intellij.plugin.editor.CurrentContextUpdater
import org.digma.intellij.plugin.idea.psi.java.JavaCodeLensService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.navigation.HomeSwitcherService
import org.digma.intellij.plugin.test.system.SingleEnvironmentData
import org.digma.intellij.plugin.test.system.DigmaTestCase
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.awt.event.MouseEvent
import java.lang.reflect.Field
import kotlin.test.assertNotEquals


class InfraTest : DigmaTestCase() {

    fun `test that getTestDataPath returns correct path`() {
        assertEquals(systemTestDataPath, myFixture.testDataPath)
    }

    fun `test that analytics service returns mocked environment`() {
        TestCase.assertEquals(environmentList, analyticsService.environments)
    }

    fun `test subscribe to documentInfoChange and openFile`() {

        var (expectedDocumentName, actualDocumentOpened) =  SingleEnvironmentData.DOC_NAME to ""
        // sub to documentInfoChange to assert that the document actually opened
        messageBusTestListeners.registerSubToDocumentInfoChangedEvent {
            actualDocumentOpened = it.name
        }

        // Open Document1
        val psiFile = myFixture.configureByFile(SingleEnvironmentData.DOC_NAME)

        waitFor(1000, "waiting for the file to open")
        // making sure that the remaining tasks in the EDT Queue are executed
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        //assert that the correct Document opened
        assertEquals(expectedDocumentName, actualDocumentOpened)
    }

    fun `_test code vision loaded from insights and click code vision`() {

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
    }

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
    }

    fun `test getting current environment and switching`() {

        var expected = environmentList[0]
        val file = myFixture.configureByFile("EditorEventsHandler.java")
        //register for environment change event
        messageBusTestListeners.registerSubToEnvironmentChangedEvent { newEnv, toRefresh ->
            Log.test(logger::info, "Test Subscriber - EnvironmentChanged: environmentChanged $newEnv")
            TestCase.assertEquals(expected, newEnv)
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
    }

    fun `test that all services are up and running`() {
        Log.test(logger::info, "Requesting analytics service")
        TestCase.assertNotNull(analyticsService)
    }

    fun `test that DocumentInfoService opens the same open file`() {
        val file = myFixture.configureByFile("TestFile.java")
        assertTrue(DocumentInfoService.getInstance(project).focusedFile == file.virtualFile)
    }

    @ParameterizedTest
    @ValueSource(strings = ["TestFile.java"])
    fun `test that DocumentInfoService switching to the open file`(fileName: String) {
        val psiFile1 = myFixture.configureByFile(fileName)

        val documentInfoService = DocumentInfoService.getInstance(project)
        assertTrue(documentInfoService.focusedFile == psiFile1.virtualFile)

        val psiFile2 = myFixture.configureByFile("TestFile.kt")
        assertTrue(documentInfoService.focusedFile == psiFile2.virtualFile)
        assertTrue(documentInfoService.focusedFile != psiFile1.virtualFile)

        myFixture.openFileInEditor(psiFile1.virtualFile)
        assertTrue(documentInfoService.focusedFile == psiFile1.virtualFile)
    }

    fun `test that file elements are correctly identified`() {

        val psiFile = myFixture.configureByFile("TestFile.java")

        val psiMethods: MutableCollection<PsiMethod> = PsiTreeUtil.findChildrenOfType(psiFile, PsiMethod::class.java)

        waitForDocumentInfoToLoad(psiFile)

        val documentInfoService = DocumentInfoService.getInstance(project)
        val documentInfoContainer: DocumentInfoContainer? = documentInfoService.getDocumentInfo(psiFile.virtualFile)

        val methods = documentInfoContainer?.documentInfo?.methods
        assertTrue(methods?.size == psiMethods.size)

        val psiMethodNames = psiMethods.map { it.name }.sorted()
        val methodNames = methods?.map { it.value.name }?.sorted()
        assertTrue(methodNames == psiMethodNames)

    }

    fun `test that method is selected according to caret position`() {
        Log.test(logger::info, "opening file")
        val psiFile = myFixture.configureByFile("TestFile.java")

        waitFor(1000, "events after opening ${psiFile.name}")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        Log.test(logger::info, "finished dispatching events")

        val editor = myFixture.editor

        val methods = PsiTreeUtil.findChildrenOfType(psiFile, PsiMethod::class.java)
        val targetMethod = methods.find { it.name == "method1" }

        targetMethod?.let {
            val offset = targetMethod.textOffset
            editor.caretModel.moveToOffset(offset)
            val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
            document?.let {
                Log.test(logger::info, "setting caret position")
                val caretAndSelectionState = EditorTestUtil.extractCaretAndSelectionMarkers(document)
                EditorTestUtil.setCaretsAndSelection(editor, caretAndSelectionState)
            }
        }
        waitFor(1000, "caret event in ${psiFile.name}")
        Log.test(logger::info, "selected method = ${project.getService(CurrentContextUpdater::class.java).latestMethodUnderCaret}")

    }

    override fun getTestProjectFileNames(): Array<String> {
        return arrayOf("TestFile.java", "TestFile.kt", "TestFile2.java")
    }
}

