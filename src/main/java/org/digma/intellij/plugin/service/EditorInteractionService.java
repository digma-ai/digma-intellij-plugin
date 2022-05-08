package org.digma.intellij.plugin.service;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.editor.EditorEventsHandler;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.MethodUnderCaret;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.toolwindow.ToolWindowContent;
import org.digma.intellij.plugin.ui.MethodContextUpdater;
import org.jetbrains.annotations.NotNull;

/**
 * A service to implement the interactions between listeners and UI components.
 * All work should be done in the EDT.
 */
public class EditorInteractionService implements MethodContextUpdater, Disposable {

    private static final Logger LOGGER = Logger.getInstance(EditorInteractionService.class);

    private ToolWindowContent toolWindowContent;
    private Project project;

    public static EditorInteractionService getInstance(Project project) {
        return project.getService(EditorInteractionService.class);
    }

    @Override
    public void updateViewContent(MethodUnderCaret methodUnderCaret) {
        if (toolWindowContent != null) {
            if (methodUnderCaret != null) {
                Log.log(LOGGER::debug, "got method under caret {}", methodUnderCaret.getId());
                toolWindowContent.update(methodUnderCaret.toString());
            } else {
                clearViewContent();
            }
        }
    }

    @Override
    public void clearViewContent() {
        if (toolWindowContent != null) {
            toolWindowContent.empty();
        }
    }

    @Override
    public void dispose() {
        Log.log(LOGGER::debug, "disposing..");
    }


    public void start(@NotNull Project project, ToolWindowContent toolWindowContent) {
        Log.log(LOGGER::debug, "starting..");
        this.project = project;
        this.toolWindowContent = toolWindowContent;
        EditorEventsHandler editorEventsHandler = project.getService(EditorEventsHandler.class);
        LanguageService languageService = project.getService(LanguageService.class);
        editorEventsHandler.start(project,this,languageService);
    }

}
