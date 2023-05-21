package org.digma.intellij.plugin.test.system;

import com.intellij.openapi.components.ComponentManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.digma.intellij.plugin.editor.EditorEventsHandler;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.psi.LanguageServiceLocator;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Paths;

public class EditorEventsHandlerTestJ extends LightJavaCodeInsightFixtureTestCase {

    private static final Logger LOGGER = Logger.getInstance(EditorEventsHandler.class);

    @Test
    public void testPsiFileNotEmpty() {
        myFixture.setTestDataPath(Paths.get("src/test/resources").toAbsolutePath().toString());
        PsiFile psiFile = myFixture.configureByFile("EditorEventsHandler.java");
        Assert.assertNotNull("PsiFile should not be null", psiFile);
        Assert.assertFalse("PsiFile should not be empty", psiFile.getText().isEmpty());

        VirtualFile newFile = new LightVirtualFile("EditorEventsHandler.java");
        Project project = getProject();
        ComponentManager componentManager = project.getComponent(ComponentManager.class);
        LanguageServiceLocator languageServiceLocator = componentManager.getService(LanguageServiceLocator.class);
        LanguageService languageService = languageServiceLocator.locate(psiFile.getLanguage());
        Log.log(LOGGER::debug, "Found language service {} for :{}",languageService, newFile);

        DocumentInfo documentInfo = languageService.buildDocumentInfo(psiFile);
        Log.log(LOGGER::debug, "got DocumentInfo for :{}", newFile);

    }

}
