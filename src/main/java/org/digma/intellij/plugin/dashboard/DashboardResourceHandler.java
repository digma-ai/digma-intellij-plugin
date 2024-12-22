package org.digma.intellij.plugin.dashboard;

import com.intellij.openapi.project.Project;
import org.cef.browser.CefBrowser;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.ui.jcef.*;
import org.jetbrains.annotations.*;

import java.io.InputStream;
import java.util.Collections;

public class DashboardResourceHandler extends BaseResourceHandler {


    public DashboardResourceHandler(@NotNull CefBrowser browser, @NotNull String path) {
        super(path, browser);
    }

    @NotNull
    @Override
    public String getResourceFolderName() {
        return DashboardConstants.DASHBOARD_RESOURCE_FOLDER_NAME;
    }

    @Nullable
    @Override
    public InputStream buildEnvJsFromTemplate(@NotNull String path) {
        Project project = JBcefBrowserPropertiesKt.getProject(getBrowser());
        if (project == null) {
            Log.log(getLogger()::warn, "project is null , should never happen");
            ErrorReporter.getInstance().reportError(null, "DashboardResourceHandler.buildEnvJsFromTemplate", "project is null", Collections.emptyMap());
            return null;
        }
        DashboardVirtualFile file = (DashboardVirtualFile) JBcefBrowserPropertiesKt.getProperty(getBrowser(), JBcefBrowserPropertiesKt.JCEF_DASHBOARD_FILE_PROPERTY_NAME);
        if (file == null) {
            Log.log(getLogger()::warn, "DashboardVirtualFile is null , should never happen");
            ErrorReporter.getInstance().reportError(null, "DashboardResourceHandler.buildEnvJsFromTemplate", "DashboardVirtualFile is null", Collections.emptyMap());
            return null;
        }
        return new DashboardEnvJsTemplateBuilder(file, path).build(project);
    }
}
