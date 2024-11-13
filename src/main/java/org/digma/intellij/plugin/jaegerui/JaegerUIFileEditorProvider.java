package org.digma.intellij.plugin.jaegerui;

import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.*;

public class JaegerUIFileEditorProvider implements FileEditorProvider {

    public static final String JAEGER_UI_EDITOR_TYPE = "Digma.JAEGER_UI_EDITOR_TYPE";


    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return JaegerUIVirtualFile.isJaegerUIVirtualFile(file);
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        //unchecked cast must succeed or we have a bug
        return new JaegerUIFileEditor(project, (JaegerUIVirtualFile) file);
    }

    @Override
    public @NotNull @NonNls String getEditorTypeId() {
        return JAEGER_UI_EDITOR_TYPE;
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.NONE;
    }


}
