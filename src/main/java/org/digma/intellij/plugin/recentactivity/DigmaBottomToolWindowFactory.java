package org.digma.intellij.plugin.recentactivity;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.log.Log;
import org.jetbrains.annotations.NotNull;


/**
 * Digma tool window inside bottom panel
 */
public class DigmaBottomToolWindowFactory implements ToolWindowFactory {
    private static final Logger LOGGER = Logger.getInstance(DigmaBottomToolWindowFactory.class);


    /**
     * this is the starting point of the plugin. this method is called when the tool window is opened.
     * before the window is opened there may be no reason to do anything, listen to events for example will be
     * a waste if the user didn't open the window. at least as much as possible, some extensions will be registered
     * but will do nothing if the plugin is not active.
     * after the plugin is active all listeners and extensions are installed and kicking until the IDE is closed.
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        Log.log(LOGGER::debug, "createToolWindowContent for project  {}", project);

        //initialize AnalyticsService early so the UI can detect the connection status when created
        AnalyticsService.getInstance(project);

        var recentActivityService = RecentActivityService.getInstance(project);

        RecentActivityToolWindowShower.getInstance(project).setToolWindow(toolWindow);

        var content = recentActivityService.createTabContent();
        if (content == null)
            return;

        toolWindow.getContentManager().addContent(content);
    }
}
