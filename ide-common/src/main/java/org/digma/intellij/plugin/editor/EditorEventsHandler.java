package org.digma.intellij.plugin.editor;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.ui.MethodContextUpdater;
import org.jetbrains.annotations.NotNull;

public interface EditorEventsHandler {

    void start(@NotNull Project project, MethodContextUpdater methodContextUpdated, LanguageService languageService);

}
