package org.digma.jetbrains.plugin.toolwindow;

import com.intellij.openapi.project.*;
import com.intellij.openapi.wm.*;
import com.intellij.ui.content.*;
import org.digma.jetbrains.plugin.service.*;
import org.jetbrains.annotations.*;


/**
 * The main Digma tool window
 */
public class DigmaToolWindowFactory implements ToolWindowFactory {


    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        EditorInteractionService editorInteractionService = EditorInteractionService.getInstance(project);
        ToolWindowContent toolWindowContent = new ToolWindowContent();
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(toolWindowContent.getContent(), "DigmaContent", false);
        toolWindow.getContentManager().addContent(content);
        editorInteractionService.init(toolWindowContent,project);
    }
}
