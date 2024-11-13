package org.digma.intellij.plugin.dashboard;

import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.*;


public class DashboardFileEditorProvider implements FileEditorProvider {

    public static final String DASHBOARD_EDITOR_TYPE = "Digma.DASHBOARD_EDITOR_TYPE";

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return DashboardVirtualFile.isDashboardVirtualFile(file);
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return new DashboardFileEditor(project, file);
    }

    @Override
    public void disposeEditor(@NotNull FileEditor editor) {
        FileEditorProvider.super.disposeEditor(editor);
    }

    @Override
    public @NotNull @NonNls String getEditorTypeId() {
        return DASHBOARD_EDITOR_TYPE;
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.NONE;
    }


}

