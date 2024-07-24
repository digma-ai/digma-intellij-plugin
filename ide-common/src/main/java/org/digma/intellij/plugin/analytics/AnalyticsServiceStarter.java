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
        Backgroundable.executeOnPooledThread(() -> {
            AnalyticsService.getInstance(project);
            //make sure BackendInfoHolder is initialized after AnalyticsService
            BackendInfoHolder.getInstance(project);
        });
    }
}
