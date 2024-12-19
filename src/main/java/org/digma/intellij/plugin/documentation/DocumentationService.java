package org.digma.intellij.plugin.documentation;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.common.EDT;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.reload.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static org.digma.intellij.plugin.documentation.DocumentationVirtualFile.DOCUMENTATION_EDITOR_KEY;

@Service(Service.Level.PROJECT)
public final class DocumentationService implements Disposable, ReloadableJCefContainer {

    private final Logger logger = Logger.getInstance(DocumentationService.class);

    private final Project project;


    public DocumentationService(Project project) {
        this.project = project;
        ApplicationManager.getApplication().getService(ReloadService.class).register(this, this);
    }

    @Override
    public void dispose() {
        //nothing to do, used as parent disposable
    }

    public static DocumentationService getInstance(Project project) {
        return project.getService(DocumentationService.class);
    }

    public void openDocumentation(@NotNull String documentationPage) {

        Log.log(logger::trace, "open documentation {}", documentationPage);

        if (showExisting(documentationPage)) {
            return;
        }

        EDT.ensureEDT(() -> {
            var file = DocumentationVirtualFile.createVirtualFile(documentationPage);
            FileEditorManager.getInstance(project).openFile(file, true, true);
        });

    }


    private boolean showExisting(String documentationPage) {
        for (var editor : FileEditorManager.getInstance(project).getAllEditors()) {
            var file = editor.getFile();
            if (file != null && DocumentationVirtualFile.isDocumentationVirtualFile(file)) {
                DocumentationVirtualFile openFile = (DocumentationVirtualFile) file;
                if (Objects.equals(openFile.getDocumentationPage(), documentationPage)) {
                    EDT.ensureEDT(() -> FileEditorManager.getInstance(project).openFile(file, true, true));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void reload() {

        Log.log(logger::trace, "reload called, reloading all documentation files");

        var files = Arrays.stream(FileEditorManager.getInstance(project).getOpenFiles()).filter(DocumentationVirtualFile::isDocumentationVirtualFile).toList();

        var newFiles = new ArrayList<DocumentationVirtualFile>();

        files.forEach(oldFile -> {
            if (oldFile instanceof DocumentationVirtualFile documentationVirtualFile) {
                var newFile = new DocumentationVirtualFile(documentationVirtualFile.getDocumentationPage());
                newFile.setDocumentationPage(documentationVirtualFile.getDocumentationPage());
                DOCUMENTATION_EDITOR_KEY.set(newFile, DocumentationFileEditorProvider.DOCUMENTATION_EDITOR_TYPE);
                newFiles.add(newFile);
            }
        });


        files.forEach(virtualFile -> {
            if (virtualFile instanceof DocumentationVirtualFile documentationVirtualFile) {
                documentationVirtualFile.setValid(false);
                FileEditorManager.getInstance(project).closeFile(documentationVirtualFile);
            }
        });

        newFiles.forEach(file -> FileEditorManager.getInstance(project).openFile(file, true, true));
    }

    @NotNull
    @Override
    public Project getProject() {
        return project;
    }
}
