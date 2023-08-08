package org.digma.intellij.plugin.recentactivity;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.digma.intellij.plugin.PluginId;
import org.digma.intellij.plugin.common.EDT;
import org.digma.intellij.plugin.log.Log;
import org.jetbrains.annotations.NotNull;

public class RecentActivityToolWindowShower {

    private static final Logger LOGGER = Logger.getInstance(RecentActivityToolWindowShower.class);


    public static RecentActivityToolWindowShower getInstance(@NotNull Project project) {
        return project.getService(RecentActivityToolWindowShower.class);
    }

    private final Project project;

    private ToolWindow toolWindow;

    public RecentActivityToolWindowShower(Project project) {
        this.project = project;
    }

    public void setToolWindow(ToolWindow toolWindow) {
        this.toolWindow = toolWindow;
    }



    public void showToolWindow() {

        Log.log(LOGGER::debug, "showToolWindow invoked");

        EDT.ensureEDT(() -> {
            if (toolWindow != null) {
                Log.log(LOGGER::debug, "Got reference to tool window, showing..");
                show(toolWindow);
            } else {
                Log.log(LOGGER::debug, "Don't have reference to tool window, showing with ToolWindowManager..");
                ToolWindow tw = ToolWindowManager.getInstance(project).getToolWindow(PluginId.OBSERVABILITY_WINDOW_ID);
                if (tw != null) {
                    Log.log(LOGGER::debug, "Got tool window from ToolWindowManager");
                    show(tw);
                } else {
                    Log.log(LOGGER::debug, "Could not find tool window");
                }
            }
        });
    }



    private void show(ToolWindow toolWindow) {
        if (toolWindow.isVisible()) {
            Log.log(LOGGER::debug, "Tool window is already visible");
        } else {
            Log.log(LOGGER::debug, "Calling toolWindow.show");
            toolWindow.show();
        }
    }
}
