package org.digma.intellij.plugin.test.system

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assert
import org.junit.Test
import java.nio.file.Paths

class EditorEventsHandlerTestK : BasePlatformTestCase() {

    @Test
    fun `test PSI file not empty`() {
        myFixture.testDataPath = Paths.get("src/test/resources").toAbsolutePath().toString()
        val psiFile = myFixture.configureByFile("EditorEventsHandler.java")
        Assert.assertNotNull("PsiFile should not be null", psiFile)
        Assert.assertFalse("PsiFile should not be empty", psiFile.text.isEmpty())
    }

}
