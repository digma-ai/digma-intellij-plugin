package org.digma.intellij.plugin.common;

import com.intellij.ide.util.RunOnceUtil;
import com.intellij.openapi.project.*;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.session.SessionMetadataProperties;
import org.digma.intellij.plugin.startup.DigmaProjectActivity;
import org.digma.intellij.plugin.ui.ToolWindowShower;
import org.jetbrains.annotations.NotNull;

import static org.digma.intellij.plugin.session.SessionMetadataPropertiesKt.getPluginLoadedKey;

public class StartupStartupActivity extends DigmaProjectActivity {

    private static final String RUN_ONCE_ID = "org.digma.runonce.opentoolwindow";


    @Override
    public void executeProjectStartup(@NotNull Project project) {
        RunOnceUtil.runOnceForApp(RUN_ONCE_ID, () -> {
            ActivityMonitor.getInstance(project).registerFirstTimePluginLoaded();
            project.getService(DumbService.class).runWhenSmart(() -> EDT.ensureEDT(() -> ToolWindowShower.getInstance(project).showToolWindow()));
        });
        ActivityMonitor.getInstance(project).registerPluginLoaded();
        SessionMetadataProperties.getInstance().put(getPluginLoadedKey(project), true);
    }

}
