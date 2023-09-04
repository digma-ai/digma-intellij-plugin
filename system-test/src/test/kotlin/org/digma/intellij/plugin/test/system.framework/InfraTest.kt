package org.digma.intellij.plugin.test.system.framework

import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import junit.framework.TestCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.digma.intellij.plugin.document.DocumentInfoService
import org.digma.intellij.plugin.idea.psi.java.JavaCodeLensService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.navigation.HomeSwitcherService
import org.digma.intellij.plugin.test.system.DigmaTestCase
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

        runBlocking {
            while (!assertFinished) {
                delay(100)
            }
        }
        TestCase.assertTrue(assertFinished)
        assertTrue("This is intended to be called, remove the line to properly test", isTheSameName)
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

}

