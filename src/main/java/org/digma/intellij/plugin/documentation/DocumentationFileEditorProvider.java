package org.digma.intellij.plugin.documentation;

import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.JBCefApp;
import org.jetbrains.annotations.*;

public class DocumentationFileEditorProvider implements FileEditorProvider {

    public static final String DOCUMENTATION_EDITOR_TYPE = "Digma.DOCUMENTATION_EDITOR_TYPE";


    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return JBCefApp.isSupported() && DocumentationVirtualFile.isDocumentationVirtualFile(file);
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return new DocumentationFileEditor(project, (DocumentationVirtualFile) file);
    }

    @Override
    public void disposeEditor(@NotNull FileEditor editor) {
        FileEditorProvider.super.disposeEditor(editor);
    }


    @Override
    public @NotNull @NonNls String getEditorTypeId() {
        return DOCUMENTATION_EDITOR_TYPE;
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.NONE;
    }


}
