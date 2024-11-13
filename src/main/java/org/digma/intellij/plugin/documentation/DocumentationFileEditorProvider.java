package org.digma.intellij.plugin.documentation;

import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.*;

public class DocumentationFileEditorProvider implements FileEditorProvider {

    public static final String DOCUMENTATION_EDITOR_TYPE = "Digma.DOCUMENTATION_EDITOR_TYPE";


    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return DocumentationVirtualFile.isDocumentationVirtualFile(file);
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return new DocumentationFileEditor(project, (DocumentationVirtualFile) file);
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
