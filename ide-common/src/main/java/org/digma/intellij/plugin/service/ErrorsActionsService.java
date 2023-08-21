package org.digma.intellij.plugin.service;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

public class ErrorsActionsService{

    private final EditorService editorService;

    public ErrorsActionsService(Project project) {
        editorService = EditorService.getInstance(project);
    }


    public void openErrorFrameWorkspaceFile(@Nullable String workspaceUrl, @Nullable String lastInstanceCommitId, int lineNumber) {
        if (workspaceUrl != null) {
            editorService.openErrorFrameWorkspaceFileInEditor(workspaceUrl, lastInstanceCommitId, lineNumber);
        }
    }

    public void openRawStackTrace(String stackTrace) {
         editorService.openRawStackTrace(stackTrace);
    }
}
