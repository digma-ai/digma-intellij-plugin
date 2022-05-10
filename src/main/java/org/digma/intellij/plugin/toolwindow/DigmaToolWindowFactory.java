package org.digma.intellij.plugin.toolwindow;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.service.EditorInteractionService;
import org.digma.intellij.plugin.ui.errors.ErrorsPanelKt;
import org.digma.intellij.plugin.ui.insights.InsightsPanelKt;
import org.digma.intellij.plugin.ui.service.ErrorsService;
import org.digma.intellij.plugin.ui.service.InsightsService;
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
            var insightsPanel = InsightsPanelKt.insightsPanel(project);
            var insightsService = project.getService(InsightsService.class);
            insightsService.setModel(InsightsPanelKt.getInsightsModel());
            insightsService.setPanel(insightsPanel);
            var insightsContent = contentFactory.createContent(insightsPanel, "Insights", false);
            toolWindow.getContentManager().addContent(insightsContent);
        }

        {
            var errorsPanel = ErrorsPanelKt.errorsPanel(project);
            var errorsService = project.getService(ErrorsService.class);
            errorsService.setModel(ErrorsPanelKt.getErrorsModel());
            errorsService.setPanel(errorsPanel);
            var errorsContent = contentFactory.createContent(errorsPanel, "Errors", false);
            toolWindow.getContentManager().addContent(errorsContent);
        }


        EditorInteractionService.getInstance(project).start(project);
    }
}
