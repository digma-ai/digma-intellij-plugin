package org.digma.intellij.plugin.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.openapi.wm.impl.ToolWindowManagerImpl;
import org.digma.intellij.plugin.PluginId;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.jetbrains.annotations.NotNull;

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
    public void stateChanged(@NotNull ToolWindowManager toolWindowManager, @NotNull ToolWindowManagerEventType changeType) {
        if (toolWindowManager instanceof ToolWindowManagerImpl &&
                changeType == ToolWindowManagerEventType.HideToolWindow) {

            var toolWindowId = toolWindowManager.getActiveToolWindowId();
            if(toolWindowId == null)
                return;

            if (toolWindowId.equals(PluginId.TOOL_WINDOW_ID)) {
                ActivityMonitor.getInstance(project).registerSidePanelClosed();
            }

            if (toolWindowId.equals(PluginId.OBSERVABILITY_WINDOW_ID)) {
                ActivityMonitor.getInstance(project).registerObservabilityPanelClosed();
            }
        }
    }
}