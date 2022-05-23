package org.digma.intellij.plugin.service;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.model.rest.insights.ErrorInsight;
import org.digma.intellij.plugin.ui.service.ErrorsViewService;
import org.digma.intellij.plugin.ui.service.InsightsViewService;
import org.jetbrains.annotations.NotNull;

public class InsightsActionsService {

    private Project project;
    private final InsightsViewService insightsViewService;
    private final ErrorsViewService errorsViewService;

    public InsightsActionsService(Project project) {
        this.project = project;
        insightsViewService = project.getService(InsightsViewService.class);
        errorsViewService = project.getService(ErrorsViewService.class);
    }


    public void errorsExpandButtonClicked(@NotNull ErrorInsight modelObject) {

        //todo: update errors model
        errorsViewService.setVisible(true);
    }
}
