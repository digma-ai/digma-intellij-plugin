package org.digma.intellij.plugin.test.system

import com.intellij.openapi.diagnostic.Logger
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.digma.intellij.plugin.editor.EditorEventsHandler
import org.digma.intellij.plugin.psi.LanguageServiceLocator
import org.junit.Test

class EditorEventsHandlerTestK : BasePlatformTestCase() {

    private val logger = Logger.getInstance(EditorEventsHandler::class.java)
    override fun getTestDataPath(): String? {
        return "src/test/resources"
    }

    @Test
    fun `test PSI file not empty`() {
        val psiFile = myFixture.configureByFile("EditorEventsHandler.java")

        val project = project
        val languageServiceLocator = project.getService(
            LanguageServiceLocator::class.java
        )

        val languageService = languageServiceLocator.locate(psiFile.language)
        logger.debug("Found language service {} for :{}", languageService, psiFile)

        val (fileUri, methods) = languageService.buildDocumentInfo(psiFile)
        logger.debug("Got DocumentInfo for :{}", psiFile)
    }

}
