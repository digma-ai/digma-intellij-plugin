package org.digma.intellij.plugin.rider.psi.csharp;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.messages.MessageBusConnection;
import com.jetbrains.rdclient.util.idea.LifetimedProjectComponent;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.document.DocumentAnalyzer;
import org.digma.intellij.plugin.document.DocumentCodeObjectsChanged;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.rider.protocol.CodeObjectHost;
import org.jetbrains.annotations.NotNull;

public class CSharpDocumentAnalyzer extends LifetimedProjectComponent implements DocumentAnalyzer , DocumentCodeObjectsChanged {

    private final Logger LOGGER = Logger.getInstance(CSharpDocumentAnalyzer.class);

    private final CodeObjectHost codeObjectHost;
    private final DocumentInfoService documentInfoService;

    private final MessageBusConnection messageBusConnection;

    public CSharpDocumentAnalyzer(Project project) {
        super(project);
        this.codeObjectHost = project.getService(CodeObjectHost.class);
        this.documentInfoService = project.getService(DocumentInfoService.class);
        messageBusConnection = project.getMessageBus().connect();
        messageBusConnection.subscribe(DocumentCodeObjectsChanged.DOCUMENT_CODE_OBJECTS_CHANGED_TOPIC,this);
    }

    @Override
    public void analyzeDocument(@NotNull PsiFile psiFile) {

        Backgroundable.ensureBackground(getProject(), "Digma: analyzeDocument", new Runnable() {
            @Override
            public void run() {
                DocumentInfo documentInfo = codeObjectHost.getDocument(psiFile);
                if (documentInfo == null) {
                    Log.log(LOGGER::error, "Could not find document for psi file {}", psiFile.getVirtualFile());
                    throw new DocumentInfoNotFoundException("Could not find document for psi file " + psiFile.getVirtualFile());
                }
                Log.log(LOGGER::debug, "Found document for {},{}", psiFile.getVirtualFile(), documentInfo);
                documentInfoService.addCodeObjects(psiFile, documentInfo);
            }
        });
    }

    //this is the event for rider when a document is opened
    @Override
    public void documentCodeObjectsChanged(@NotNull PsiFile psiFile) {
        Log.log(LOGGER::debug, "Got documentCodeObjectsChanged event for {}", psiFile.getVirtualFile());
        analyzeDocument(psiFile);
    }

    @Override
    public void dispose() {
        super.dispose();
        messageBusConnection.dispose();
    }
}
