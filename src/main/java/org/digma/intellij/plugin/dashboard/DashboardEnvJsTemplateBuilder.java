package org.digma.intellij.plugin.dashboard;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.ui.jcef.BaseEnvJsTemplateBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static org.digma.intellij.plugin.analytics.EnvUtilsKt.getCurrentEnvironmentId;

class DashboardEnvJsTemplateBuilder extends BaseEnvJsTemplateBuilder {

    private static final String DASHBOARD_ENVIRONMENT = "dashboardEnvironment";
    private static final String INITIAL_ROUTE_PARAM_NAME = "initial_route";

    private final DashboardVirtualFile dashboardVirtualFile;

    public DashboardEnvJsTemplateBuilder(DashboardVirtualFile dashboardVirtualFile, String templatePath) {
        super(templatePath);
        this.dashboardVirtualFile = dashboardVirtualFile;
    }


    @Override
    public void addAppSpecificEnvVariable(@NotNull Project project, @NotNull Map<String, Object> data) {
        var envId = getCurrentEnvironmentId(project);
        data.put(DASHBOARD_ENVIRONMENT, envId == null ? "undefined" : envId);
        data.put(INITIAL_ROUTE_PARAM_NAME, dashboardVirtualFile.getInitialRoute());
    }

}
