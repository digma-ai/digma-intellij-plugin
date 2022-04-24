package org.digma.intellij.plugin.editor;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.ui.MethodContextUpdater;
import org.digma.rider.protocol.ElementUnderCaretService;
import org.jetbrains.annotations.NotNull;

public class RiderEditorEventsHandler implements EditorEventsHandler{

    private Project project;

    public RiderEditorEventsHandler(Project project) {
        this.project = project;
    }

    @Override
    public void start(@NotNull Project project, MethodContextUpdater methodContextUpdated, LanguageService languageService) {
        ElementUnderCaretService elementUnderCaretService = project.getService(ElementUnderCaretService.class);
        elementUnderCaretService.start(methodContextUpdated);
    }
}
