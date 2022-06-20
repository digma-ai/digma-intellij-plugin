package org.digma.intellij.plugin.service;

import com.intellij.openapi.project.Project;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError;
import org.digma.intellij.plugin.model.rest.insights.ErrorInsightNamedError;
import org.digma.intellij.plugin.ui.service.ErrorsViewService;
import org.digma.intellij.plugin.ui.service.InsightsViewService;
import org.digma.intellij.plugin.ui.service.ToolWindowTabsHelper;
import org.jetbrains.annotations.NotNull;

public class ErrorsActionsService implements ContentManagerListener {

    private final Project project;
    private final InsightsViewService insightsViewService;
    private final ErrorsViewService errorsViewService;
    private final ToolWindowTabsHelper toolWindowTabsHelper;

    public ErrorsActionsService(Project project) {
        this.project = project;
        insightsViewService = project.getService(InsightsViewService.class);
        errorsViewService = project.getService(ErrorsViewService.class);
        toolWindowTabsHelper = project.getService(ToolWindowTabsHelper.class);
    }



    public void showErrorDetails(@NotNull ErrorInsightNamedError error) {
        showErrorDetails(error.getUid());
    }

    public void showErrorDetails(@NotNull CodeObjectError codeObjectError) {
        showErrorDetails(codeObjectError.getUid());
    }

    private void showErrorDetails(@NotNull String uid) {
        toolWindowTabsHelper.showingErrorDetails();
        errorsViewService.showErrorDetails(uid);
        errorsViewService.setVisible();
        toolWindowTabsHelper.errorDetailsOn();
    }

    public void closeErrorDetails() {
        toolWindowTabsHelper.errorDetailsOff();
        errorsViewService.closeErrorDetails();
        insightsViewService.updateUi();
        toolWindowTabsHelper.errorDetailsClosed();
    }

    @Override
    public void selectionChanged(@NotNull ContentManagerEvent event) {
        if (toolWindowTabsHelper.isErrorDetailsOn() && event.getContent().getTabName().equalsIgnoreCase("insights")){
            toolWindowTabsHelper.errorDetailsOff();
            errorsViewService.closeErrorDetails();
            insightsViewService.updateUi();
        }
    }
}
