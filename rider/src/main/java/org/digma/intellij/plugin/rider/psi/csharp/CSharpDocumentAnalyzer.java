package org.digma.intellij.plugin.rider.psi.csharp;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.document.DocumentAnalyzer;
import org.digma.intellij.plugin.document.DocumentCodeObjectsChanged;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.rider.protocol.CodeObjectHost;

public class CSharpDocumentAnalyzer implements DocumentAnalyzer , DocumentCodeObjectsChanged {

    private final CodeObjectHost codeObjectHost;
    private final DocumentInfoService documentInfoService;
    private Project project;

    public CSharpDocumentAnalyzer(Project project) {
        this.project = project;
        this.codeObjectHost = project.getService(CodeObjectHost.class);
        this.documentInfoService = project.getService(DocumentInfoService.class);

        project.getMessageBus().connect().subscribe(DocumentCodeObjectsChanged.DOCUMENT_CODE_OBJECTS_CHANGED_TOPIC,this);
    }

    @Override
    public void fileOpened(PsiFile psiFile) {
        DocumentInfo documentInfo = codeObjectHost.getDocument(psiFile);
        documentInfoService.addCodeObjects(psiFile,documentInfo);
    }

    //this is the event for rider when a document is opened
    @Override
    public void documentCodeObjectsChanged(PsiFile psiFile) {
       fileOpened(psiFile);
    }
}
