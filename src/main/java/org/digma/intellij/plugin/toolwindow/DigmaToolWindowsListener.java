package org.digma.intellij.plugin.toolwindow;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.*;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import org.digma.intellij.plugin.PluginId;
import org.digma.intellij.plugin.activation.UserActivationService;
import org.digma.intellij.plugin.docker.LocalInstallationFacade;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.ui.notificationcenter.AppNotificationCenter;
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
            UserActivationService.getInstance().mainToolWindowShown(project);
        }
        if (toolWindow.getId().equals(PluginId.OBSERVABILITY_WINDOW_ID)) {
            ActivityMonitor.getInstance(toolWindow.getProject()).registerObservabilityPanelOpened();
            UserActivationService.getInstance().recentActivityToolWindowShown(project);
        }
    }


    @Override
    public void stateChanged(@NotNull ToolWindowManager toolWindowManager, @NotNull ToolWindowManagerEventType changeType) {

        if (changeType == ToolWindowManagerEventType.HideToolWindow) {

            var toolWindowId = toolWindowManager.getActiveToolWindowId();
            if (toolWindowId == null)
                return;

            if (toolWindowId.equals(PluginId.TOOL_WINDOW_ID)) {
                onMainToolWindowClose();
                UserActivationService.getInstance().mainToolWindowHidden(project);
            }

            if (toolWindowId.equals(PluginId.OBSERVABILITY_WINDOW_ID)) {
                onObservabilityToolWindowClose();
                UserActivationService.getInstance().recentActivityToolWindowHidden(project);
            }
        }
    }


    private void onMainToolWindowClose() {
        ActivityMonitor.getInstance(project).registerSidePanelClosed();

        if (LocalInstallationFacade.getInstance().isInstallationInProgress()) {
            ApplicationManager.getApplication().getService(AppNotificationCenter.class).showInstallationInProgressNotification(project);
        }
    }


    private void onObservabilityToolWindowClose() {
        ActivityMonitor.getInstance(project).registerObservabilityPanelClosed();
    }

}