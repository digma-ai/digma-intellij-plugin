package org.digma.intellij.plugin.dashboard;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.startup.DigmaProjectActivity;
import org.jetbrains.annotations.NotNull;

public class DashboardServiceStarter extends DigmaProjectActivity {

    @Override
    public void executeProjectStartup(@NotNull Project project) {
        //need to start DashboardService so that it will start listening to events
        DashboardService.getInstance(project);
    }
}
