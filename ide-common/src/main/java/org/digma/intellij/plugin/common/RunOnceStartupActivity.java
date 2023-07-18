package org.digma.intellij.plugin.common;

import com.intellij.ide.util.RunOnceUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.ui.ToolWindowShower;
import org.jetbrains.annotations.NotNull;

public class RunOnceStartupActivity implements StartupActivity {

    private static final String RUN_ONCE_ID = "org.digma.runonce.opentoolwindow";



    /*
    run setFirstTimePluginLoaded only once and show tool window after first load of the plugin,
    probably after installation.
    to test that:
    the persistence file DigmaPersistence.xml is application persistence and will be under jetbrains config folder,
    in linux its ./.config/JetBrains/IdeaIC2023.1/options/DigmaPersistence.xml.
    when running the development instance it will be in the sandbox ander the build folder.
    delete the file or run ./gradlew clean.
    the run once file is other.xml, and it will be under jetbrains cache directory, in linux It's somewhere under
    ~/config/JetBrains. when running the development instance it will be under build/idea-sandbox/config/options/other.xml.
    delete the file.
    and test.


     */
    @Override
    public void runActivity(@NotNull Project project) {
        RunOnceUtil.runOnceForApp(RUN_ONCE_ID, () -> {
            ActivityMonitor.getInstance(project).registerFirstTimePluginLoaded();
            EDT.ensureEDT(() -> ToolWindowShower.getInstance(project).showToolWindow());
        });
    }
}
