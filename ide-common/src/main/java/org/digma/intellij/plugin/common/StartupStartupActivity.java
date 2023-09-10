package org.digma.intellij.plugin.common;

import com.intellij.ide.util.RunOnceUtil;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.ui.ToolWindowShower;
import org.jetbrains.annotations.NotNull;

public class StartupStartupActivity implements StartupActivity.DumbAware {

    private static final String RUN_ONCE_ID = "org.digma.runonce.opentoolwindow";

    @Override
    public void runActivity(@NotNull Project project) {
        RunOnceUtil.runOnceForApp(RUN_ONCE_ID, () -> {
            ActivityMonitor.getInstance(project).registerFirstTimePluginLoaded();
            project.getService(DumbService.class).runWhenSmart(() -> EDT.ensureEDT(() -> ToolWindowShower.getInstance(project).showToolWindow()));
        });
        ActivityMonitor.getInstance(project).registerPluginLoaded();
    }
}
