package org.digma.intellij.plugin.toolwindow;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.service.EditorInteractionService;
import org.digma.intellij.plugin.ui.errors.ErrorsTabKt;
import org.digma.intellij.plugin.ui.insights.InsightsTabKt;
import org.digma.intellij.plugin.ui.service.ErrorsViewService;
import org.digma.intellij.plugin.ui.service.InsightsViewService;
import org.jetbrains.annotations.NotNull;


/**
 * The main Digma tool window
 */
public class DigmaToolWindowFactory implements ToolWindowFactory {

    private static final Logger LOGGER = Logger.getInstance(DigmaToolWindowFactory.class);

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

        var contentFactory = ContentFactory.SERVICE.getInstance();

        {
            var insightsPanel = InsightsTabKt.insightsPanel(project);
            var insightsViewService = project.getService(InsightsViewService.class);
            insightsViewService.setModel(InsightsTabKt.getInsightsModel());
            insightsViewService.setPanel(insightsPanel);
            var insightsContent = contentFactory.createContent(insightsPanel, "Insights", false);
            toolWindow.getContentManager().addContent(insightsContent);
        }

        {
            var errorsPanel = ErrorsTabKt.errorsPanel(project);
            var errorsViewService = project.getService(ErrorsViewService.class);
            errorsViewService.setModel(ErrorsTabKt.getErrorsModel());
            errorsViewService.setPanel(errorsPanel);
            var errorsContent = contentFactory.createContent(errorsPanel, "Errors", false);
            toolWindow.getContentManager().addContent(errorsContent);
            errorsViewService.setContent(toolWindow,errorsContent);
        }


        EditorInteractionService.getInstance(project).start(project);
    }
}
