package org.digma.intellij.plugin.document;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.analytics.AnalyticsProvider;
import org.digma.intellij.plugin.model.DocumentInfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DocumentInfoService {

    private final Project project;
    private final AnalyticsProvider analyticsProvider;

    //temp
    private final String environment = "UNSET_ENV";

    private final Map<PsiFile, DocumentInfoContainer> documents = Collections.synchronizedMap(new HashMap<>());

    public DocumentInfoService(Project project) {
        this.project = project;
        analyticsProvider = project.getService(AnalyticsProvider.class);
    }



    public void notifyDocumentInfoChanged(PsiFile psiFile) {
        DocumentInfoChanged publisher = project.getMessageBus().syncPublisher(DocumentInfoChanged.DOCUMENT_INFO_CHANGED_TOPIC);
        publisher.documentInfoChanged(psiFile);
    }


    //called after a document is analyzed for code objects
    public void addCodeObjects(PsiFile psiFile, DocumentInfo documentInfo) {
        DocumentInfoContainer documentInfoContainer =  documents.computeIfAbsent(psiFile, psiFile1 -> new DocumentInfoContainer(psiFile1));
        documentInfoContainer.update(documentInfo,analyticsProvider,environment);
        notifyDocumentInfoChanged(psiFile);
    }



    public DocumentInfoContainer getDocumentInfo(PsiFile psiFile) {
        return documents.get(psiFile);
    }


}
