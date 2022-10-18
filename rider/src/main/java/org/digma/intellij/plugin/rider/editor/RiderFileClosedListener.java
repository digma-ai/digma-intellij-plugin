package org.digma.intellij.plugin.rider.editor;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.rider.protocol.CodeObjectHost;
import org.jetbrains.annotations.NotNull;

public class RiderFileClosedListener implements FileEditorManagerListener {

    private final Project project;
    private final CodeObjectHost codeObjectHost;
    private final DocumentInfoService documentInfoService;
    private final LanguageService cSharpLanguageService;


    public RiderFileClosedListener(Project project) {
        this.project = project;
        this.codeObjectHost = project.getService(CodeObjectHost.class);
        this.documentInfoService = project.getService(DocumentInfoService.class);
        this.cSharpLanguageService = project.getService(LanguageService.class);
    }

    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {

        //when a file is closed or deleted while open we want to clear its document from the rider protocol and from
        //and the DocumentInfo from documentInfoService
        var psiFile = PsiManager.getInstance(project).findFile(file);

        if (psiFile != null && cSharpLanguageService.isSupportedFile(project, psiFile)) {
            documentInfoService.removeDocumentInfo(psiFile);
            codeObjectHost.removeDocument(psiFile);
        }
    }
}
