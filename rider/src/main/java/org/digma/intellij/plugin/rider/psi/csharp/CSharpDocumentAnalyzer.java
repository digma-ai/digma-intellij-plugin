package org.digma.intellij.plugin.rider.psi.csharp;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.document.DocumentAnalyzer;
import org.digma.intellij.plugin.document.DocumentCodeObjectsChanged;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.rider.protocol.CodeObjectHost;
import org.jetbrains.annotations.NotNull;

public class CSharpDocumentAnalyzer implements DocumentAnalyzer , DocumentCodeObjectsChanged {

    private static final Logger LOGGER = Logger.getInstance(CSharpDocumentAnalyzer.class);

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
    public void fileOpened(@NotNull PsiFile psiFile) {
        DocumentInfo documentInfo = codeObjectHost.getDocument(psiFile);
        Log.log(LOGGER::debug, "Found document for {},{}",psiFile.getVirtualFile(),documentInfo);
        documentInfoService.addCodeObjects(psiFile,documentInfo);
    }

    //this is the event for rider when a document is opened
    @Override
    public void documentCodeObjectsChanged(@NotNull PsiFile psiFile) {
        Log.log(LOGGER::debug, "Got documentCodeObjectsChanged event for {}", psiFile.getVirtualFile());
        fileOpened(psiFile);
    }
}
