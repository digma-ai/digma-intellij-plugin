package org.digma.intellij.plugin.documentation;

import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import org.digma.intellij.plugin.common.DigmaVirtualFileMarker;
import org.jetbrains.annotations.NotNull;

public class DocumentationVirtualFile extends LightVirtualFile implements DigmaVirtualFileMarker {

    public static final Key<String> DOCUMENTATION_EDITOR_KEY = Key.create("Digma.DOCUMENTATION_EDITOR_KEY");
    private String documentationPage;

    public DocumentationVirtualFile(String myTitle) {
        super(myTitle);
        setFileType(DocumentationFileType.INSTANCE);
        setWritable(false);
        putUserData(FileEditorManagerImpl.FORBID_PREVIEW_TAB, true);
    }

    public static boolean isDocumentationVirtualFile(@NotNull VirtualFile file) {
        return file instanceof DocumentationVirtualFile;
    }

    @NotNull
    public static VirtualFile createVirtualFile(@NotNull String documentationPage) {
        //todo: other title
        var file = new DocumentationVirtualFile(documentationPage);
        file.setDocumentationPage(documentationPage);
        DOCUMENTATION_EDITOR_KEY.set(file, DocumentationFileEditorProvider.DOCUMENTATION_EDITOR_TYPE);
        return file;
    }

    public void setDocumentationPage(String documentationPage) {
        this.documentationPage = documentationPage;
    }

    public String getDocumentationPage() {
        return documentationPage;
    }
}
