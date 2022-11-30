package org.digma.intellij.plugin.idea.psi.java;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

//todo: implement bulk change
public class FileDocumentManagerListenerForJavaSpanNavigation implements FileDocumentManagerListener {


    @Override
    public void beforeAllDocumentsSaving() {
        FileDocumentManagerListener.super.beforeAllDocumentsSaving();
    }

    @Override
    public void beforeDocumentSaving(@NotNull Document document) {
        FileDocumentManagerListener.super.beforeDocumentSaving(document);
    }

    @Override
    public void beforeFileContentReload(@NotNull VirtualFile file, @NotNull Document document) {
        FileDocumentManagerListener.super.beforeFileContentReload(file, document);
    }

    @Override
    public void fileWithNoDocumentChanged(@NotNull VirtualFile file) {
        FileDocumentManagerListener.super.fileWithNoDocumentChanged(file);
    }

    @Override
    public void fileContentReloaded(@NotNull VirtualFile file, @NotNull Document document) {
        FileDocumentManagerListener.super.fileContentReloaded(file, document);
    }

    @Override
    public void fileContentLoaded(@NotNull VirtualFile file, @NotNull Document document) {
        FileDocumentManagerListener.super.fileContentLoaded(file, document);
    }

    @Override
    public void unsavedDocumentDropped(@NotNull Document document) {
        FileDocumentManagerListener.super.unsavedDocumentDropped(document);
    }

    @Override
    public void unsavedDocumentsDropped() {
        FileDocumentManagerListener.super.unsavedDocumentsDropped();
    }

    @Override
    public void afterDocumentUnbound(@NotNull VirtualFile file, @NotNull Document document) {
        FileDocumentManagerListener.super.afterDocumentUnbound(file, document);
    }
}
