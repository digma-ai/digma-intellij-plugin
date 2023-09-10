package org.digma.intellij.plugin.test.system

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.EditorTestUtil
import junit.framework.TestCase
import org.digma.intellij.plugin.document.DocumentInfoContainer
import org.digma.intellij.plugin.document.DocumentInfoService
import org.digma.intellij.plugin.editor.CurrentContextUpdater
import org.digma.intellij.plugin.log.Log
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource


class SystemTest : DigmaTestCase() {

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
        val psiFile = myFixture.configureByFile("TestFile.java")
        FileEditorManager.getInstance(project).openFile(psiFile.virtualFile, true)
        val editor = myFixture.editor

        val methods = PsiTreeUtil.findChildrenOfType(psiFile, PsiMethod::class.java)
        val targetMethod = methods.find { it.name == "method1" }

        targetMethod?.let {
            val offset = targetMethod.textOffset
            editor.caretModel.moveToOffset(offset)
            val endOffset = editor.caretModel.visualLineEnd
            val startOffset = editor.caretModel.visualLineStart
            editor.selectionModel.setSelection(startOffset, endOffset)
            val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
            document?.let {
                val caretAndSelectionState = EditorTestUtil.extractCaretAndSelectionMarkers(document)
                EditorTestUtil.setCaretsAndSelection(editor, caretAndSelectionState)
            }
        }
        Log.test(logger::info, "selected method = ${project.getService(CurrentContextUpdater::class.java).latestMethodUnderCaret}")
    }

}
