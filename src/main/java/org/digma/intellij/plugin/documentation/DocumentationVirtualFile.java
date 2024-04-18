package org.digma.intellij.plugin.documentation;

import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import org.digma.intellij.plugin.common.DigmaVirtualFileMarker;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class DocumentationVirtualFile extends LightVirtualFile implements DigmaVirtualFileMarker {

    public static final Key<String> DOCUMENTATION_EDITOR_KEY = Key.create("Digma.DOCUMENTATION_EDITOR_KEY");

    private static final Map<String, String> titles = Map.of(
            "run-digma-with-terminal", "Digma in the Terminal",
            "run-digma-with-docker", "Digma with Docker",
            "run-digma-with-gradle-tasks", "Digma using gradle",
            "environment-types", "Insights Overview"
    );

    private String documentationPage;

    private static String getTitle(String documentationPage)
    {
        if(titles.containsKey(documentationPage)){
            return titles.get(documentationPage);
        }
        return documentationPage;
    }

    public DocumentationVirtualFile(String documentationPage) {
        super(getTitle(documentationPage));
        this.documentationPage = documentationPage;
        setFileType(DocumentationFileType.INSTANCE);
        setWritable(false);
        putUserData(FileEditorManagerImpl.FORBID_PREVIEW_TAB, true);
    }

    public static boolean isDocumentationVirtualFile(@NotNull VirtualFile file) {
        return file instanceof DocumentationVirtualFile;
    }

    @NotNull
    public static VirtualFile createVirtualFile(@NotNull String documentationPage) {
        var file = new DocumentationVirtualFile(documentationPage);
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
