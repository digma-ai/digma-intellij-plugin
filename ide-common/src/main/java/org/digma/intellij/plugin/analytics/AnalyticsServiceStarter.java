package org.digma.intellij.plugin.analytics;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.digma.intellij.plugin.common.Backgroundable;
import org.jetbrains.annotations.NotNull;

/**
 * Initialize AnalyticsService early as possible
 */
public class AnalyticsServiceStarter implements StartupActivity.DumbAware {

    @Override
    public void runActivity(@NotNull Project project) {
        Backgroundable.ensureBackground(project, "initializing analytics service", () -> {
            AnalyticsService.getInstance(project);
            //make sure BackendInfoHolder is initialized after AnalyticsService
            BackendInfoHolder.getInstance();
        });
    }
}
