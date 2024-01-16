package org.digma.intellij.plugin.test.system.framework

import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.PlatformTestUtil
import junit.framework.TestCase
import org.digma.intellij.plugin.document.DocumentInfoContainer
import org.digma.intellij.plugin.document.DocumentInfoService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.test.system.SingleEnvironmentData
import org.digma.intellij.plugin.test.system.DigmaTestCase
import org.digma.intellij.plugin.test.system.TwoEnvironmentSecondFileNavigateToCodeData
import org.digma.intellij.plugin.test.system.TwoEnvironmentsFirstFileRelatedSingleSpanData
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource


class InfraTest : DigmaTestCase() {

    override fun setUp() {
        super.setUp()
        mockGetEnvironments(mockAnalyticsProvider, SingleEnvironmentData.environmentList)
        mockGetInsightsOfMethods(mockAnalyticsProvider, SingleEnvironmentData.expectedInsightsOfMethods)
        mockGetRecentActivity(mockAnalyticsProvider, SingleEnvironmentData.expectedRecentActivityResult)
        mockGetInsightOfSingeSpan(mockAnalyticsProvider, TwoEnvironmentsFirstFileRelatedSingleSpanData.expectedInsightOfSingleSpan) // those insights are for both envs
        mockGetCodeObjectNavigation(mockAnalyticsProvider, TwoEnvironmentSecondFileNavigateToCodeData.codeObjectNavigation)
    }

    fun `test that getTestDataPath returns correct path`() {
        assertEquals(systemTestDataPath, myFixture.testDataPath)
    }

    fun `test that analytics service returns mocked environment`() {
        TestCase.assertEquals(SingleEnvironmentData.environmentList, analyticsService.environments)
    }

    //this test will fail the SoW test when running together in the same test batch.
    fun `_test subscribe to documentInfoChange and openFile`() {

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
        Log.test(logger::info, "selected method = ${getMethodUnderCaret()}")

    }

    override fun getTestProjectFileNames(): Array<String> {
        return arrayOf("TestFile.java", "TestFile.kt", "TestFile2.java")
    }
}

