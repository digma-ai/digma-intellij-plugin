package org.digma.intellij.plugin.toolwindow.common;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import org.digma.intellij.plugin.PluginId;
import org.digma.intellij.plugin.persistence.PersistenceService;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.ui.ToolWindowShower;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DigmaToolWindowsListener implements ToolWindowManagerListener {

    private final Project project;

    public DigmaToolWindowsListener(Project project) {
        this.project = project;
    }

    @Override
    public void toolWindowShown(@NotNull ToolWindow toolWindow) {
        if (toolWindow.getId().equals(PluginId.TOOL_WINDOW_ID)) {
            ActivityMonitor.getInstance(toolWindow.getProject()).registerSidePanelOpened();
        }
        if (toolWindow.getId().equals(PluginId.OBSERVABILITY_WINDOW_ID)) {
            ActivityMonitor.getInstance(toolWindow.getProject()).registerObservabilityPanelOpened();
        }
    }

    @Override
    public void toolWindowsRegistered(@NotNull List<String> ids, @NotNull ToolWindowManager toolWindowManager) {
        if (ids.contains(PluginId.TOOL_WINDOW_ID) &&
                ! PersistenceService.getInstance().getState().getFirstTimePluginLoaded()) {
            ToolWindowShower.getInstance(project).showToolWindow();
            ActivityMonitor.getInstance(project).registerFirstTimePluginLoaded();
            PersistenceService.getInstance().getState().setFirstTimePluginLoaded(true);
        }
    }
}