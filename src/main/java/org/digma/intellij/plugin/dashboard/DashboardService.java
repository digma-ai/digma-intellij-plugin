package org.digma.intellij.plugin.dashboard;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;

import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.AnalyticsServiceException;
import org.digma.intellij.plugin.common.EDT;
import org.digma.intellij.plugin.dashboard.incoming.GoToSpan;
import org.digma.intellij.plugin.insights.InsightsViewOrchestrator;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.navigation.HomeSwitcherService;
import org.digma.intellij.plugin.navigation.InsightsAndErrorsTabsHelper;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.posthog.MonitoredPanel;
import org.digma.intellij.plugin.ui.MainToolWindowCardsController;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

@Service(Service.Level.PROJECT)
public final class DashboardService {

    private final Logger logger = Logger.getInstance(DashboardService.class);
    private final Project project;
    public DashboardService(Project project) {
        this.project = project;
    }

    public static DashboardService getInstance(Project project) {
        return project.getService(DashboardService.class);
    }

    public boolean isIndexHtml(String path) {
        return path.endsWith("index.html");
    }

    public InputStream buildIndexFromTemplate(String path, DashboardVirtualFile dashboardVirtualFile) {

        if (!isIndexHtml(path)) {
            //should not happen
            return null;
        }

        return new DashboardIndexTemplateBuilder().build(project, dashboardVirtualFile);
    }


    public void openDashboard(@NotNull String dashboardName) {

        ActivityMonitor.getInstance(project).openDashboardButtonClicked("DashboardButton");

        if (showExisting(dashboardName)) {
            return;
        }

        EDT.ensureEDT(() -> {
            var file = DashboardVirtualFile.createVirtualFile(dashboardName);
            FileEditorManager.getInstance(project).openFile(file, true, true);
        });

    }

    private boolean showExisting(@NotNull String dashboardName) {
        for (var editor : FileEditorManager.getInstance(project).getAllEditors()) {
            var file = editor.getFile();
            if (file != null && DashboardVirtualFile.isDashboardVirtualFile(file)) {
                DashboardVirtualFile openFile = (DashboardVirtualFile) file;
                if (Objects.equals(openFile.getDashboardEnvId(), dashboardName)) {
                    EDT.ensureEDT(() -> FileEditorManager.getInstance(project).openFile(file, true, true));
                    return true;
                }
            }
        }
        return false;
    }

    @NotNull
    public String getDashboard(@NotNull Map<String,String> queryParams) throws AnalyticsServiceException {
        return AnalyticsService.getInstance(project).getDashboard(queryParams);
    }

    public void goToSpanAndNavigateToCode(GoToSpan goToSpan) {

        Log.log(logger::debug, project, "goToSpan request {}", goToSpan);

        var span = goToSpan.payload();

        Log.log(logger::debug, project, "calling showInsightsForSpanOrMethodAndNavigateToCode from goToSpan for {}", span);

        EDT.ensureEDT(() -> {
            MainToolWindowCardsController.getInstance(project).closeAllNotificationsIfShowing();
            project.getService(HomeSwitcherService.class).switchToInsights();
            project.getService(InsightsAndErrorsTabsHelper.class).switchToInsightsTab();
            ActivityMonitor.getInstance(project).registerSpanLinkClicked(MonitoredPanel.Dashboard);
            project.getService(InsightsViewOrchestrator.class).showInsightsForCodelessSpan(span.spanCodeObjectId());
        });
    }
}
