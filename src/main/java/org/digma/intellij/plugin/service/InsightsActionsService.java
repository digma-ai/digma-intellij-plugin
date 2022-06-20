package org.digma.intellij.plugin.service;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.model.rest.insights.ErrorInsight;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.ui.service.ErrorsViewService;
import org.digma.intellij.plugin.ui.service.InsightsViewService;
import org.jetbrains.annotations.NotNull;

public class InsightsActionsService {

    private final Project project;
    private final InsightsViewService insightsViewService;
    private final ErrorsViewService errorsViewService;
    private final LanguageService languageService;

    public InsightsActionsService(Project project) {
        this.project = project;
        insightsViewService = project.getService(InsightsViewService.class);
        errorsViewService = project.getService(ErrorsViewService.class);
        languageService = project.getService(LanguageService.class);
    }


    public void showErrorsTab(@NotNull ErrorInsight modelObject) {
        errorsViewService.setVisible();
    }

    public void navigateToMethod(@NotNull String codeObjectId) {
        languageService.navigateToMethod(codeObjectId);
    }
}
