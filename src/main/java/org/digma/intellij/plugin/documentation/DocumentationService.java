package org.digma.intellij.plugin.documentation;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.common.EDT;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.Objects;


@Service(Service.Level.PROJECT)
public final class DocumentationService {

    private final Logger logger = Logger.getInstance(DocumentationService.class);

    private final Project project;


    public DocumentationService(Project project) {
        this.project = project;
    }


    public static DocumentationService getInstance(Project project) {
        return project.getService(DocumentationService.class);
    }

    boolean isIndexHtml(String path) {
        return path.endsWith("index.html");
    }

    public InputStream buildIndexFromTemplate(String path, DocumentationVirtualFile documentationVirtualFile) {

        if (!isIndexHtml(path)) {
            //should not happen
            return null;
        }

        return new DocumentationIndexTemplateBuilder().build(project, documentationVirtualFile);
    }


    public void openDocumentation(@NotNull String documentationPage) {

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
//
//
//    private boolean showExisting(String traceId, String spanName) {
//        for (var editor : FileEditorManager.getInstance(project).getAllEditors()) {
//            var file = editor.getFile();
//            if (file != null && JaegerUIVirtualFile.isJaegerUIVirtualFile(file)) {
//                JaegerUIVirtualFile openFile = (JaegerUIVirtualFile) file;
//                if (Objects.equals(openFile.getSpanName(), spanName) && Objects.equals(openFile.getTraceId(), traceId)) {
//                    EDT.ensureEDT(() -> FileEditorManager.getInstance(project).openFile(file, true, true));
//                    return true;
//                }
//            }
//        }
//        return false;
//    }


}
