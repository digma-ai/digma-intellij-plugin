package org.digma.intellij.plugin.service;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError;
import org.digma.intellij.plugin.model.rest.insights.ErrorInsightNamedError;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.ui.service.ErrorsViewService;
import org.digma.intellij.plugin.ui.service.InsightsViewService;
import org.jetbrains.annotations.NotNull;

public class ErrorsActionsService {

    private final Project project;
    private final InsightsViewService insightsViewService;
    private final ErrorsViewService errorsViewService;
    private final LanguageService languageService;

    public ErrorsActionsService(Project project) {
        this.project = project;
        insightsViewService = project.getService(InsightsViewService.class);
        errorsViewService = project.getService(ErrorsViewService.class);
        languageService = project.getService(LanguageService.class);
    }



    public void showErrorDetails(@NotNull ErrorInsightNamedError error) {
        errorsViewService.showErrorDetails(error.getUid());
        errorsViewService.setVisible();
    }

    public void showErrorDetails(@NotNull CodeObjectError codeObjectError) {
        errorsViewService.showErrorDetails(codeObjectError.getUid());
        errorsViewService.setVisible();
    }
}
