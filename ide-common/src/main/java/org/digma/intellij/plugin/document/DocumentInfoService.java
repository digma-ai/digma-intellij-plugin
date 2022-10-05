package org.digma.intellij.plugin.document;

import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.apache.commons.lang3.mutable.MutableInt;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.digma.intellij.plugin.psi.PsiFileNotFountException;
import org.digma.intellij.plugin.psi.PsiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * DocumentInfoService holds the open documents containers and has various services to query the documents containers.
 * it knows nothing about the logic behind , timings of queries for DocumentInfo objects
 * should be managed elsewhere, for that reason DocumentInfoService may return null if queried
 * for an object that doesn't exist in its documents list.
 */
public class DocumentInfoService {

    private final Logger LOGGER = Logger.getInstance(DocumentInfoService.class);

    private final Project project;
    private final AnalyticsService analyticsService;

    private final Map<PsiFile, DocumentInfoContainer> documents = Collections.synchronizedMap(new HashMap<>());

    //dominantLanguages keeps track of the most used programming language in the documents. it is used to decide which
    // language service to use when we don't have a method info.
    //using IdentityHashMap here because the key must and will always be different objects. the overhead of hashCode
    //and equals is not necessary here.
    private final Map<Language, MutableInt> dominantLanguages = new IdentityHashMap<>();

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
        Log.log(LOGGER::debug, "Adding DocumentInfo for {},{}",psiFile.getVirtualFile(),documentInfo);
        DocumentInfoContainer documentInfoContainer = documents.computeIfAbsent(psiFile, file -> new DocumentInfoContainer(file, analyticsService));
        documentInfoContainer.update(documentInfo);
        dominantLanguages.computeIfAbsent(documentInfoContainer.getLanguage(), key -> new MutableInt(0)).increment();
        notifyDocumentInfoChanged(psiFile);
    }

    public void removeDocumentInfo(@NotNull PsiFile psiFile) {
        Log.log(LOGGER::debug, "Removing document for PsiFile {}", psiFile.getVirtualFile());
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


    //this method must return a result, this codeObjectId must exist. otherwise it's a bug
    @NotNull
    public Language getLanguageByMethodCodeObjectId(String codeObjectId) {
        Optional<DocumentInfoContainer> optional = this.documents.values().stream().
                filter(container -> container.getDocumentInfo().getMethods().containsKey(codeObjectId)).
                findAny();
        if (optional.isPresent()) {
            return optional.get().getLanguage();
        }
        throw new RuntimeException("could not find language by method id " + codeObjectId);
    }

    public Language getDominantLanguage() {

        Language language = null;
        int max = 0;
        for (Map.Entry<Language, MutableInt> entry : dominantLanguages.entrySet()) {
            Language lang = entry.getKey();
            MutableInt mutableInt = entry.getValue();
            if (mutableInt.intValue() > max) {
                max = mutableInt.intValue();
                language = lang;
            }
        }

        return language;
    }
}
