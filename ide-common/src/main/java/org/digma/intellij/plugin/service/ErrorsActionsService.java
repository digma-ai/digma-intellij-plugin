package org.digma.intellij.plugin.service;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.common.EDT;
import org.digma.intellij.plugin.errors.ErrorsProvider;
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError;
import org.digma.intellij.plugin.model.rest.insights.ErrorInsightNamedError;
import org.digma.intellij.plugin.navigation.InsightsAndErrorsTabsHelper;
import org.digma.intellij.plugin.ui.model.EmptyScope;
import org.digma.intellij.plugin.ui.model.Scope;
import org.digma.intellij.plugin.ui.model.UIInsightsStatus;
import org.digma.intellij.plugin.ui.service.ErrorsViewService;
import org.digma.intellij.plugin.ui.service.InsightsViewService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ErrorsActionsService{

    private final Project project;
    private final InsightsViewService insightsViewService;
    private final ErrorsViewService errorsViewService;
    private final InsightsAndErrorsTabsHelper insightsAndErrorsTabsHelper;

    private Scope scopeBeforeErrorDetails = null;

    private final EditorService editorService;
    private UIInsightsStatus statusBeforeErrorDetails;

    public ErrorsActionsService(Project project) {
        this.project = project;
        insightsViewService = project.getService(InsightsViewService.class);
        errorsViewService = project.getService(ErrorsViewService.class);
        insightsAndErrorsTabsHelper = project.getService(InsightsAndErrorsTabsHelper.class);
        editorService = project.getService(EditorService.class);
    }



    public void showErrorDetails(@NotNull ErrorInsightNamedError error) {
        showErrorDetails(error.getUid(),false);
    }

    public void showErrorDetails(@NotNull CodeObjectError codeObjectError) {
        showErrorDetails(codeObjectError.getUid(),false);
    }

    public void showErrorDetailsFromDashboard(@NotNull String uid){
        showErrorDetails(uid,true);
    }

    //todo: move to insightsViewOrchestrator

    /*
    rememberCurrentScope is necessary when clicking an error in the dashboard. if the errors tab currently
    showing errors of some method then the scope will be replaced to the error scope that is going to show,
    and the previous scope restored when error details is closed.
     */
    private void showErrorDetails(@NotNull String uid,boolean rememberCurrentScope) {

        //maybe there is already an error showing, must set to off before updating the model \
        // because AbstractViewService.canUpdateUI will not let update if errorDetailsOn
        insightsAndErrorsTabsHelper.errorDetailsOffNoTitleChange();

        insightsAndErrorsTabsHelper.rememberCurrentTab();
        insightsAndErrorsTabsHelper.switchToErrorsTab();

        ErrorsProvider errorsProvider  = project.getService(ErrorsProvider.class);

        boolean replaceScope = false;
        if (rememberCurrentScope || insightsViewService.getModel().getScope() instanceof EmptyScope) {
            scopeBeforeErrorDetails = insightsViewService.getModel().getScope();
            statusBeforeErrorDetails = insightsViewService.getModel().getStatus();
            replaceScope = true;
        }else {
            scopeBeforeErrorDetails = null;
            statusBeforeErrorDetails = null;
        }

        var finalReplaceScope = replaceScope;
        Backgroundable.ensureBackground(project, "Show error details", () -> {
            errorsViewService.showErrorDetails(uid, errorsProvider, finalReplaceScope);
            //this is necessary so the scope line will update with the error scope
            insightsViewService.notifyModelChangedAndUpdateUi();

            EDT.ensureEDT(insightsAndErrorsTabsHelper::errorDetailsOn);

        });


    }

    public void closeErrorDetailsBackButton() {
        closeErrorDetails();

    }

    public void closeErrorDetails() {
        insightsAndErrorsTabsHelper.errorDetailsOff();
        errorsViewService.closeErrorDetails();
        insightsAndErrorsTabsHelper.errorDetailsClosed();
        if (scopeBeforeErrorDetails != null && statusBeforeErrorDetails != null) {
            insightsViewService.getModel().setScope(scopeBeforeErrorDetails);
            insightsViewService.getModel().setStatus(statusBeforeErrorDetails);
            insightsViewService.notifyModelChangedAndUpdateUi();
            scopeBeforeErrorDetails = null;
            statusBeforeErrorDetails = null;
        }
        errorsViewService.updateUi();
        insightsViewService.updateUi();
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
