package org.digma.intellij.plugin.analytics;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.startup.DigmaProjectActivity;
import org.jetbrains.annotations.NotNull;

/**
 * Initialize AnalyticsService early as possible
 */
public class AnalyticsServiceStarter extends DigmaProjectActivity {


    @Override
    public void executeProjectStartup(@NotNull Project project) {
        Backgroundable.ensureBackgroundWithoutReadAccess(project, "initializing analytics service", () -> {
            //start AnalyticsServiceSettingsWatcher so it will start listening to settings change events
            AnalyticsServiceSettingsWatcher.getInstance();

            AnalyticsService.getInstance(project);
            //make sure BackendInfoHolder is initialized after AnalyticsService
            BackendInfoHolder.getInstance(project).loadOnStartup();
        });
    }
}
