package org.digma.intellij.plugin.document;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.digma.intellij.plugin.psi.PsiFileNotFountException;
import org.digma.intellij.plugin.psi.PsiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * DocumentInfoService acts as a container for DocumentInfo objects.
 * it knows nothing about the logic behind , timings of queries for DocumentInfo objects
 * should be managed elsewhere, for that reason DocumentInfoService may return null if queried
 * for an object that doesn't exist in its documents list.
 */
public class DocumentInfoService {

    private final Logger LOGGER = Logger.getInstance(DocumentInfoService.class);

    private final Project project;
    private final AnalyticsService analyticsService;

    private final Map<PsiFile, DocumentInfoContainer> documents = Collections.synchronizedMap(new HashMap<>());

    public DocumentInfoService(Project project) {
        this.project = project;
        analyticsService = project.getService(AnalyticsService.class);
    }


    public Set<PsiFile> allKeys() {
        return documents.keySet();
    }


    public void environmentChanged(String newEnv) {
        Log.log(LOGGER::debug, "Got environmentChanged event {}", newEnv);

        //refresh all backend data.
        //must run in background
        documents.forEach((psiFile, container) -> {
            container.refresh();
        });
    }

    public void notifyDocumentInfoChanged(PsiFile psiFile) {
        Log.log(LOGGER::debug, "Notifying DocumentInfo changed for {}",psiFile.getVirtualFile());
        DocumentInfoChanged publisher = project.getMessageBus().syncPublisher(DocumentInfoChanged.DOCUMENT_INFO_CHANGED_TOPIC);
        publisher.documentInfoChanged(psiFile);
    }


    //called after a document is analyzed for code objects
    public void addCodeObjects(@NotNull PsiFile psiFile, @NotNull DocumentInfo documentInfo) {
        Log.log(LOGGER::debug, "Adding DocumentInfo for {},{}", psiFile.getVirtualFile(), documentInfo);
        DocumentInfoContainer documentInfoContainer = documents.computeIfAbsent(psiFile, psiFile1 -> new DocumentInfoContainer(psiFile1, analyticsService));
        documentInfoContainer.update(documentInfo);
        notifyDocumentInfoChanged(psiFile);
    }

    public void removeDocumentInfo(@NotNull PsiFile psiFile) {
        documents.remove(psiFile);
    }

    @Nullable
    public DocumentInfoContainer getDocumentInfo(PsiFile psiFile) {
        return documents.get(psiFile);
    }

    @Nullable
    public DocumentInfoContainer getDocumentInfo(MethodUnderCaret methodUnderCaret) {
        try {
            PsiFile psiFile = PsiUtils.uriToPsiFile(methodUnderCaret.getFileUri(), project);
            return getDocumentInfo(psiFile);
        } catch (PsiFileNotFountException e) {
            Log.log(LOGGER::error, "Could not locate psi file for uri {}", methodUnderCaret.getFileUri());
            return null;
        }
    }


    /*
     * getMethodInfo may return null. DocumentInfoService is just a container, it knows about what you feed it with.
     */
    @Nullable
    public MethodInfo getMethodInfo(MethodUnderCaret methodUnderCaret) {
        try {
            PsiFile psiFile = PsiUtils.uriToPsiFile(methodUnderCaret.getFileUri(), project);
            DocumentInfoContainer documentInfoContainer = documents.get(psiFile);
            return documentInfoContainer == null ? null : documentInfoContainer.getMethodInfo(methodUnderCaret.getId());
        } catch (PsiFileNotFountException e) {
            Log.log(LOGGER::error, "Could not locate psi file for uri {}", methodUnderCaret.getFileUri());
            return null;
        }
    }

    public MethodInfo findMethodInfo(String sourceCodeObjectId) {

        return this.documents.values().stream().
                filter(documentInfoContainer -> documentInfoContainer.getDocumentInfo().getMethods().containsKey(sourceCodeObjectId)).
                findAny().map(documentInfoContainer -> documentInfoContainer.getMethodInfo(sourceCodeObjectId)).
                orElse(null);
    }

}
