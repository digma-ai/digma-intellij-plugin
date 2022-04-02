package org.digma.intellij.plugin.toolwindow;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.*;
import com.intellij.openapi.wm.*;
import com.intellij.ui.content.*;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.service.*;
import org.jetbrains.annotations.*;


/**
 * The main Digma tool window
 */
public class DigmaToolWindowFactory implements ToolWindowFactory {

    private static final Logger LOGGER = Logger.getInstance(DigmaToolWindowFactory.class);

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        Log.log(LOGGER::debug, "createToolWindowContent for project  {}", project);
        EditorInteractionService editorInteractionService = EditorInteractionService.getInstance(project);
        ToolWindowContent toolWindowContent = new ToolWindowContent();
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(toolWindowContent.getContent(), "DigmaContent", false);
        toolWindow.getContentManager().addContent(content);
        editorInteractionService.init(toolWindowContent,project);
    }
}
