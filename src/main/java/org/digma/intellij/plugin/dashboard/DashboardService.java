package org.digma.intellij.plugin.dashboard;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.analytics.*;
import org.digma.intellij.plugin.common.EDT;
import org.digma.intellij.plugin.dashboard.incoming.GoToSpan;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.posthog.*;
import org.digma.intellij.plugin.scope.*;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.*;

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

    public void goToSpan(GoToSpan goToSpan) {

        Log.log(logger::debug, project, "goToSpan request {}", goToSpan);

        ActivityMonitor.getInstance(project).registerSpanLinkClicked(goToSpan.payload().spanCodeObjectId(), UserActionOrigin.Dashboard);

        var span = goToSpan.payload();

        var environmentsSupplier = AnalyticsService.getInstance(project).getEnvironment();
        environmentsSupplier.setCurrent(span.environment(), () -> ScopeManager.getInstance(project).changeScope(new SpanScope(span.spanCodeObjectId())));
    }
}
