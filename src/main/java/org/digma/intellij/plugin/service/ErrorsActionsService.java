package org.digma.intellij.plugin.service;

import com.intellij.openapi.project.Project;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import org.digma.intellij.plugin.errors.ErrorsProvider;
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError;
import org.digma.intellij.plugin.model.rest.insights.ErrorInsightNamedError;
import org.digma.intellij.plugin.ui.service.ErrorsViewService;
import org.digma.intellij.plugin.ui.service.InsightsViewService;
import org.digma.intellij.plugin.ui.service.SummaryViewService;
import org.digma.intellij.plugin.ui.service.TabsHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ErrorsActionsService implements ContentManagerListener {

    private final Project project;
    private final InsightsViewService insightsViewService;
    private final ErrorsViewService errorsViewService;
    private final SummaryViewService summaryViewService;
    private final TabsHelper tabsHelper;

    private final EditorService editorService;

    public ErrorsActionsService(Project project) {
        this.project = project;
        insightsViewService = project.getService(InsightsViewService.class);
        errorsViewService = project.getService(ErrorsViewService.class);
        summaryViewService = project.getService(SummaryViewService.class);
        tabsHelper = project.getService(TabsHelper.class);
        editorService = project.getService(EditorService.class);
    }



    public void showErrorDetails(@NotNull ErrorInsightNamedError error) {
        showErrorDetails(error.getUid());
    }

    public void showErrorDetails(@NotNull CodeObjectError codeObjectError) {
        showErrorDetails(codeObjectError.getUid());
    }

    public void showErrorDetails(@NotNull String uid) {
        tabsHelper.showingErrorDetails();
        errorsViewService.setVisible();
        ErrorsProvider errorsProvider  = project.getService(ErrorsProvider.class);
        errorsViewService.showErrorDetails(uid,errorsProvider);
        tabsHelper.errorDetailsOn();
    }

    public void closeErrorDetailsBackButton() {
        closeErrorDetailsWithoutNotify();
        tabsHelper.errorDetailsClosed();
    }

    public void closeErrorDetailsWithoutNotify() {
        tabsHelper.errorDetailsOff();
        errorsViewService.closeErrorDetails();
        insightsViewService.updateUi();
    }


    @Override
    public void selectionChanged(@NotNull ContentManagerEvent event) {
        if (tabsHelper.isErrorDetailsOn() &&
                (tabsHelper.isInsightsTab(event.getContent()) || tabsHelper.isSummaryTab(event.getContent()))) {
            tabsHelper.errorDetailsOff();
            errorsViewService.closeErrorDetails();
            insightsViewService.updateUi();
            summaryViewService.updateUi();
        }
    }

    public void openErrorFrameWorkspaceFile(@Nullable String workspaceUrl, @Nullable String lastInstanceCommitId, int lineNumber) {
        if (workspaceUrl != null) {
            editorService.openErrorFrameWorkspaceFileInEditor(workspaceUrl, lastInstanceCommitId, lineNumber);
        }
    }

    public void openRawStackTrace(String stackTrace) {
         editorService.openRawStackTrace(stackTrace);
    }
}
