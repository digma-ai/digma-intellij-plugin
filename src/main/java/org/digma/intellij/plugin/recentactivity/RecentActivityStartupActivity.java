package org.digma.intellij.plugin.recentactivity;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public class RecentActivityStartupActivity implements StartupActivity {

    @Override
    public void runActivity(@NotNull Project project) {
        RecentActivityService.getInstance(project).startFetchingActivities();
    }
}
