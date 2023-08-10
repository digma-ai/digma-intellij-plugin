package org.digma.intellij.plugin.documentation;

import com.intellij.openapi.fileTypes.ex.FakeFileType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class DocumentationFileType extends FakeFileType {

    private DocumentationFileType() {
    }

    public static final DocumentationFileType INSTANCE = new DocumentationFileType();

    @Override
    public boolean isMyFileType(@NotNull VirtualFile file) {
        return DocumentationVirtualFile.isDocumentationVirtualFile(file);
    }

    @Override
    public @NonNls @NotNull String getName() {
        return "DigmaDocs";
    }

    @Override
    public @NotNull @Nls String getDisplayName() {
        return "DigmaDocs";
    }

    @Override
    public @NlsContexts.Label @NotNull String getDescription() {
        return "DigmaDocs file type";
    }
}
