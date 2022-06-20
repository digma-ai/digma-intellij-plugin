package org.digma.intellij.plugin.service;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError;
import org.digma.intellij.plugin.model.rest.insights.ErrorInsightNamedError;
import org.digma.intellij.plugin.ui.service.ErrorsViewService;
import org.digma.intellij.plugin.ui.service.InsightsViewService;
import org.jetbrains.annotations.NotNull;

public class ErrorsActionsService {

    private final Project project;
    private final InsightsViewService insightsViewService;
    private final ErrorsViewService errorsViewService;

    private Boolean wasErrorsTabVisible = false;

    public ErrorsActionsService(Project project) {
        this.project = project;
        insightsViewService = project.getService(InsightsViewService.class);
        errorsViewService = project.getService(ErrorsViewService.class);
    }



    public void showErrorDetails(@NotNull ErrorInsightNamedError error) {
        showErrorDetails(error.getUid());
    }

    public void showErrorDetails(@NotNull CodeObjectError codeObjectError) {
        showErrorDetails(codeObjectError.getUid());
    }

    private void showErrorDetails(@NotNull String uid) {
        wasErrorsTabVisible = errorsViewService.isVisible();
        errorsViewService.showErrorDetails(uid);
        errorsViewService.setVisible();
    }

    public void errorDetailsBackButtonPressed() {
        errorsViewService.closeErrorDetails();
        insightsViewService.updateUi();
        if (wasErrorsTabVisible){
            errorsViewService.setVisible();
        }else{
            insightsViewService.setVisible();
        }
        wasErrorsTabVisible = false;
    }
}
