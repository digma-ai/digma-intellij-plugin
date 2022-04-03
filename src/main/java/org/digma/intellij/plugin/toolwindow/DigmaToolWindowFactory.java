package org.digma.intellij.plugin.toolwindow;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.service.EditorInteractionService;
import org.jetbrains.annotations.NotNull;


/**
 * The main Digma tool window
 */
public class DigmaToolWindowFactory implements ToolWindowFactory {

    private static final Logger LOGGER = Logger.getInstance(DigmaToolWindowFactory.class);

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        Log.log(LOGGER::debug, "createToolWindowContent for project  {}", project);
        var toolWindowContent = new ToolWindowContent();
        var contentFactory = ContentFactory.SERVICE.getInstance();
        var content = contentFactory.createContent(toolWindowContent.getContent(), "DigmaContent", false);
        toolWindow.getContentManager().addContent(content);
        EditorInteractionService.getInstance(project).setToolWindowContent(toolWindowContent);
    }
}
