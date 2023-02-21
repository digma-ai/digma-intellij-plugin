package org.digma.intellij.plugin.service;

import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.model.rest.insights.ErrorInsight;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.psi.LanguageServiceLocator;
import org.digma.intellij.plugin.ui.service.ErrorsViewService;
import org.digma.intellij.plugin.ui.service.InsightsViewService;
import org.jetbrains.annotations.NotNull;

public class InsightsActionsService {

    private final Project project;
    private final InsightsViewService insightsViewService;
    private final ErrorsViewService errorsViewService;
    private final EditorService editorService;
    private final DocumentInfoService documentInfoService;

    private final LanguageServiceLocator languageServiceLocator;


    public InsightsActionsService(Project project) {
        this.project = project;
        insightsViewService = project.getService(InsightsViewService.class);
        errorsViewService = project.getService(ErrorsViewService.class);
        editorService = project.getService(EditorService.class);
        documentInfoService = project.getService(DocumentInfoService.class);
        languageServiceLocator = project.getService(LanguageServiceLocator.class);
    }


    public void showErrorsTab(@NotNull ErrorInsight modelObject) {
        errorsViewService.setVisible();
    }

    /**
     * This method is called from the functions list panel and should navigate to a function.
     * it will not navigate to any method in the project.
     * @param codeObjectId the method id to navigate
     */
    public void navigateToMethodFromFunctionsListPanel(@NotNull String codeObjectId) {
        Language language = documentInfoService.getLanguageByMethodCodeObjectId(codeObjectId);
        LanguageService languageService = languageServiceLocator.locate(language);
        languageService.navigateToMethod(codeObjectId);
    }

    public void openWorkspaceFileForSpan(@NotNull String workspaceUri, int offset) {
        editorService.openSpanWorkspaceFileInEditor(workspaceUri, offset);
    }

}
