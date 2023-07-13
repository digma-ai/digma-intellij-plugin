package org.digma.intellij.plugin.test.system;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.editor.EditorEventsHandler;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.psi.LanguageServiceLocator;
import org.junit.Test;
import org.mockito.Mockito;

public class EditorEventsHandlerTestJ extends LightJavaCodeInsightFixtureTestCase {

    private static final Logger LOGGER = Logger.getInstance(EditorEventsHandler.class);
    @Override
    protected String getTestDataPath() {
        return "src/test/resources";
    }

    private AnalyticsService mockAnalyticsService;
    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Create mock AnalyticsService
        mockAnalyticsService = Mockito.mock(AnalyticsService.class);

        // Replace the real AnalyticsService with the mock
        ServiceContainerUtil.replaceService(getProject(), AnalyticsService.class, mockAnalyticsService, getTestRootDisposable());
    }

    @Test
    public void testPsiFileNotEmpty() {
        PsiFile psiFile = myFixture.configureByFile("EditorEventsHandler.java");

        Project project = getProject();
        LanguageServiceLocator languageServiceLocator = project.getService(LanguageServiceLocator.class);

        LanguageService languageService = languageServiceLocator.locate(psiFile.getLanguage());
        Log.log(LOGGER::debug, "Found language service {} for :{}",languageService, psiFile);

        DocumentInfo documentInfo = languageService.buildDocumentInfo(psiFile);
        Log.log(LOGGER::debug, "got DocumentInfo for :{}", psiFile);
    }

}
