package org.digma.intellij.plugin.dashboard;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.analytics.*;
import org.digma.intellij.plugin.common.EDT;
import org.digma.intellij.plugin.dashboard.incoming.GoToSpan;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.posthog.*;
import org.digma.intellij.plugin.reload.*;
import org.digma.intellij.plugin.scope.*;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.*;

import static org.digma.intellij.plugin.dashboard.DashboardVirtualFile.DASHBOARD_EDITOR_KEY;

@Service(Service.Level.PROJECT)
public final class DashboardService implements Disposable, ReloadableJCefContainer {

    private final Logger logger = Logger.getInstance(DashboardService.class);
    private final Project project;

    public DashboardService(Project project) {
        this.project = project;
        ApplicationManager.getApplication().getService(ReloadService.class).register(this, this);
    }

    @Override
    public void dispose() {
        //nothing to do, used as parent disposable
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

    public void openReport(@NotNull String reportName) {

        if (showExisting(reportName)) {
            return;
        }

        EDT.ensureEDT(() -> {
            var file = DashboardVirtualFile.createVirtualFile(reportName);
            file.setPath("report");
            FileEditorManager.getInstance(project).openFile(file, true, true);
        });

    }

    private boolean showExisting(@NotNull String dashboardName) {
        for (var editor : FileEditorManager.getInstance(project).getAllEditors()) {
            var file = editor.getFile();
            if (file != null && DashboardVirtualFile.isDashboardVirtualFile(file)) {
                DashboardVirtualFile openFile = (DashboardVirtualFile) file;
                if (Objects.equals(openFile.getDashboardName(), dashboardName)) {
                    EDT.ensureEDT(() -> FileEditorManager.getInstance(project).openFile(file, true, true));
                    return true;
                }
            }
        }
        return false;
    }

    @NotNull
    public String getDashboard(@NotNull Map<String, String> queryParams) throws AnalyticsServiceException {
        return AnalyticsService.getInstance(project).getDashboard(queryParams);
    }

    public void goToSpan(GoToSpan goToSpan) {

        Log.log(logger::debug, project, "goToSpan request {}", goToSpan);

        ActivityMonitor.getInstance(project).registerSpanLinkClicked(goToSpan.payload().spanCodeObjectId(), UserActionOrigin.Dashboard);

        var span = goToSpan.payload();

        ScopeManager.getInstance(project).changeScope(new SpanScope(span.spanCodeObjectId()), false, null, null, span.environment());
    }


    @Override
    public void reload() {

        var files = Arrays.stream(FileEditorManager.getInstance(project).getOpenFiles()).filter(DashboardVirtualFile::isDashboardVirtualFile).toList();

        var newFiles = new ArrayList<DashboardVirtualFile>();

        files.forEach(oldFile -> {
            if (oldFile instanceof DashboardVirtualFile dashboardVirtualFile) {
                var newFile = new DashboardVirtualFile(dashboardVirtualFile.getName());
                newFile.setDashboardName(dashboardVirtualFile.getDashboardName());
                newFile.setPath(dashboardVirtualFile.getPath());
                DASHBOARD_EDITOR_KEY.set(newFile, DashboardFileEditorProvider.DASHBOARD_EDITOR_TYPE);
                newFiles.add(newFile);
            }
        });

        files.forEach(file -> FileEditorManager.getInstance(project).closeFile(file));

        newFiles.forEach(file -> FileEditorManager.getInstance(project).openFile(file));
    }

    @NotNull
    @Override
    public Project getProject() {
        return project;
    }
}
