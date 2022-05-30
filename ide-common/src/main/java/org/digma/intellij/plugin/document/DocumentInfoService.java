package org.digma.intellij.plugin.document;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.analytics.AnalyticsProvider;
import org.digma.intellij.plugin.analytics.Environment;
import org.digma.intellij.plugin.analytics.EnvironmentChanged;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.digma.intellij.plugin.model.rest.summary.CodeObjectSummary;
import org.digma.intellij.plugin.model.rest.summary.MethodCodeObjectSummary;
import org.digma.intellij.plugin.psi.PsiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * DocumentInfoService acts as a container for DocumentInfo objects.
 * it knows nothing about the logic behind , timings of queries for DocumentInfo objects
 * should be managed elsewhere, for that reason DocumentInfoService may return null if queried
 * for an object that doesn't exist in its documents list.
 */
public class DocumentInfoService implements EnvironmentChanged {

    private static final Logger LOGGER = Logger.getInstance(DocumentInfoService.class);

    private final Project project;
    private final AnalyticsProvider analyticsProvider;
    private final Environment environment;


    private final Map<PsiFile, DocumentInfoContainer> documents = Collections.synchronizedMap(new HashMap<>());

    public DocumentInfoService(Project project) {
        this.project = project;
        analyticsProvider = project.getService(AnalyticsProvider.class);
        environment = project.getService(Environment.class);

        project.getMessageBus().connect().subscribe(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC,this);
    }


    @Override
    public void environmentChanged(String newEnv) {
        documents.clear();
    }

    public void notifyDocumentInfoChanged(PsiFile psiFile) {
        DocumentInfoChanged publisher = project.getMessageBus().syncPublisher(DocumentInfoChanged.DOCUMENT_INFO_CHANGED_TOPIC);
        publisher.documentInfoChanged(psiFile);
    }


    //called after a document is analyzed for code objects
    public void addCodeObjects(@NotNull PsiFile psiFile, @NotNull DocumentInfo documentInfo) {
        Log.log(LOGGER::info, "Adding DocumentInfo for {},{}",psiFile,documentInfo);
        DocumentInfoContainer documentInfoContainer = documents.computeIfAbsent(psiFile, DocumentInfoContainer::new);
        documentInfoContainer.update(documentInfo, analyticsProvider, environment.getCurrent());
        notifyDocumentInfoChanged(psiFile);
    }


    @Nullable
    public DocumentInfoContainer getDocumentInfo(PsiFile psiFile) {
        return documents.get(psiFile);
    }

    @Nullable
    public DocumentInfoContainer getDocumentInfo(MethodUnderCaret methodUnderCaret) {
        PsiFile psiFile = PsiUtils.uriToPsiFile(methodUnderCaret.getFileUri(), project);
        return getDocumentInfo(psiFile);
    }


    @Nullable
    public MethodCodeObjectSummary getMethodSummaries(@NotNull MethodUnderCaret methodUnderCaret) {
        String id = methodUnderCaret.getId();
        String fileUri = methodUnderCaret.getFileUri();
        PsiFile psiFile = PsiUtils.uriToPsiFile(fileUri, project);
        DocumentInfoContainer documentInfoContainer = documents.get(psiFile);
        return documentInfoContainer == null ? null : documentInfoContainer.getMethodSummaries(id);
    }


    /*
     * getMethodInfo may return null. DocumentInfoService is just a container, it knows about what you feed it with.
     */
    @Nullable
    public MethodInfo getMethodInfo(MethodUnderCaret methodUnderCaret) {
        PsiFile psiFile = PsiUtils.uriToPsiFile(methodUnderCaret.getFileUri(), project);
        DocumentInfoContainer documentInfoContainer = documents.get(psiFile);
        return documentInfoContainer == null ? null : documentInfoContainer.getMethodInfo(methodUnderCaret.getId());
    }

    public Collection<CodeObjectSummary> getDocumentSummaries(MethodUnderCaret methodUnderCaret) {
        String fileUri = methodUnderCaret.getFileUri();
        PsiFile psiFile = PsiUtils.uriToPsiFile(fileUri, project);
        DocumentInfoContainer documentInfoContainer = documents.get(psiFile);
        Map<String, CodeObjectSummary>  summaryMap = documentInfoContainer.getSummaries();
        return summaryMap.values();
    }

}
