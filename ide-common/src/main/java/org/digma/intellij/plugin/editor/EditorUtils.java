package org.digma.intellij.plugin.editor;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EditorUtils {

    private EditorUtils() {
    }

    @Nullable
    public static Editor getSelectedTextEditorForFile(VirtualFile virtualFile, @NotNull FileEditorManager fileEditorManager) {
        var selectedEditor = fileEditorManager.getSelectedEditor(virtualFile);
        if (selectedEditor instanceof TextEditor textEditor) {
            return textEditor.getEditor();
        }
        return null;
    }
}
