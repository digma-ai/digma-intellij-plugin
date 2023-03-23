package org.digma.intellij.plugin.toolwindow.sidepane;

import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import org.digma.intellij.plugin.PluginId;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.jetbrains.annotations.NotNull;

public class DigmaSidePaneToolWindowListener implements ToolWindowManagerListener {

    @Override
    public void toolWindowShown(@NotNull ToolWindow toolWindow) {
        if (toolWindow.getId().equals(PluginId.TOOL_WINDOW_ID)){
            ActivityMonitor.getInstance(toolWindow.getProject()).registerSidePanelOpened();
        }
    }
}