package org.digma.intellij.plugin.editor;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public interface EditorEventsHandler {

    void start(@NotNull Project project);

}
