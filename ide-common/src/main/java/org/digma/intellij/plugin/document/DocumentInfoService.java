package org.digma.intellij.plugin.document;

import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.apache.commons.lang3.mutable.MutableInt;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.InsightType;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult;
import org.digma.intellij.plugin.psi.PsiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

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

    /**
     * the main map of document infos.
     * it would be better to use the PsiFile object as key but Psi elements may change while a project is opened,
     * there are situations where intellij may decide to reparse a psi tree and in that case the psi file may not
     * be valid anymore, the instance in intellij indexes will change. so we use the file uri as key and always convert
     * from PsiFile to uri when necessary.
     */
    private final Map<String, DocumentInfoContainer> documents = Collections.synchronizedMap(new HashMap<>());

    //dominantLanguages keeps track of the most used programming language in the documents. it is used to decide which
    // language service to use when we don't have a method info.
    //using IdentityHashMap here because the key must and will always be different objects. the overhead of hashCode
    //and equals is not necessary here.
    private final Map<String, MutableInt> dominantLanguages = new IdentityHashMap<>();

    public DocumentInfoService(Project project) {
        this.project = project;
        analyticsService = project.getService(AnalyticsService.class);
    }


    public static DocumentInfoService getInstance(Project project){
        return project.getService(DocumentInfoService.class);
    }

    public Set<String> allKeys() {
        return documents.keySet();
    }


    public boolean contains(PsiFile psiFile) {
        return documents.containsKey(PsiUtils.psiFileToUri(psiFile));
    }


    public void environmentChanged(String newEnv) {
        Log.log(LOGGER::debug, "Got environmentChanged event {}", newEnv);

        //refresh all backend data.
        //must run in background
        updateCacheForAllOpenedDocuments();
    }


    public void updateCacheForAllOpenedDocuments(){
        documents.forEach((psiFileUri, container) -> container.updateCache());
    }

    public void updateCacheForOtherOpenedDocuments(String selectedDocumentFileUri){
        Map<String, DocumentInfoContainer> documentsToRefresh = new HashMap<>(documents);
        documentsToRefresh.remove(selectedDocumentFileUri);
        documentsToRefresh.forEach((psiFileUri, container) -> container.updateCache());
    }

    public void refreshIfExists(PsiFile psiFile) {
        DocumentInfoContainer documentInfoContainer = documents.get(PsiUtils.psiFileToUri(psiFile));
        if (documentInfoContainer != null) {
            documentInfoContainer.updateCache();
        }
    }



    public void notifyDocumentInfoChanged(PsiFile psiFile) {
        Log.log(LOGGER::debug, "Notifying DocumentInfo changed for {}",psiFile.getVirtualFile());
        DocumentInfoChanged publisher = project.getMessageBus().syncPublisher(DocumentInfoChanged.DOCUMENT_INFO_CHANGED_TOPIC);
        publisher.documentInfoChanged(psiFile);
    }


    //called after a document is analyzed for code objects
    public void addCodeObjects(@NotNull PsiFile psiFile, @NotNull DocumentInfo documentInfo) {
        Log.log(LOGGER::debug, "Adding DocumentInfo for {},{}",psiFile.getVirtualFile(),documentInfo);
        DocumentInfoContainer documentInfoContainer = documents.computeIfAbsent(PsiUtils.psiFileToUri(psiFile), file -> new DocumentInfoContainer(psiFile, analyticsService));
        documentInfoContainer.update(documentInfo);
        dominantLanguages.computeIfAbsent(documentInfoContainer.getLanguage().getID(), key -> new MutableInt(0)).increment();
        notifyDocumentInfoChanged(psiFile);
    }

    public void removeDocumentInfo(@NotNull PsiFile psiFile) {
        Log.log(LOGGER::debug, "Removing document for PsiFile {}", psiFile.getVirtualFile());
        documents.remove(PsiUtils.psiFileToUri(psiFile));
    }

    public UsageStatusResult getCachedUsageStatus(@NotNull MethodInfo methodInfo, List<String> objectIds) {
        Log.log(LOGGER::debug, "Requesting cached usage status for MethodInfo {} and for objectIds {} ", methodInfo.getId(), objectIds);

        DocumentInfoContainer documentInfoContainer = documents.get(methodInfo.getContainingFileUri());
        if (documentInfoContainer == null) {
            Log.log(LOGGER::debug, "DocumentInfoContainer is null ");
            return new UsageStatusResult(Collections.emptyList(), Collections.emptyList());
        }
        return documentInfoContainer.getUsageStatus();
    }

    public List<CodeObjectInsight> getCachedMethodInsights(@NotNull MethodInfo methodInfo) {
        Log.log(LOGGER::debug, "Requesting cached insights for MethodInfo {}", methodInfo.getId());

        DocumentInfoContainer documentInfoContainer = documents.get(methodInfo.getContainingFileUri());
        if (documentInfoContainer != null) {
            return documentInfoContainer.getAllInsights().stream().filter(codeObjectInsight -> {
                String codeObjectId = codeObjectInsight.getCodeObjectId();
                return (!codeObjectInsight.getType().equals(InsightType.Unmapped)) &&
                        (methodInfo.allIdsWithoutType().contains(codeObjectId)
                        || methodInfo.allIdsWithType().contains(codeObjectId)
                        || methodInfo.getRelatedCodeObjectIds().contains(codeObjectId)
                        || methodInfo.getRelatedCodeObjectIdsWithType().contains(codeObjectId));
            }).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public DocumentInfoContainer getDocumentInfoByMethodInfo(@NotNull MethodInfo methodInfo) {
        return documents.get(methodInfo.getContainingFileUri());
    }

    @Nullable
    public DocumentInfoContainer getDocumentInfo(PsiFile psiFile) {
        return documents.get(PsiUtils.psiFileToUri(psiFile));
    }

    @Nullable
    public DocumentInfoContainer getDocumentInfo(MethodUnderCaret methodUnderCaret) {
        return documents.get(methodUnderCaret.getFileUri());
    }

    @Nullable
    public DocumentInfoContainer getDocumentInfo(String fileUri) {
        return documents.get(fileUri);
    }


    /*
     * getMethodInfo may return null. DocumentInfoService is just a container, it knows about what you feed it with.
     */
    @Nullable
    public MethodInfo getMethodInfo(MethodUnderCaret methodUnderCaret) {
            DocumentInfoContainer documentInfoContainer = documents.get(methodUnderCaret.getFileUri());
            return documentInfoContainer == null ? null : documentInfoContainer.getMethodInfo(methodUnderCaret.getId());
    }

    public MethodInfo findMethodInfo(String sourceCodeObjectId) {
        return this.documents.values().stream().
                filter(documentInfoContainer -> documentInfoContainer.getDocumentInfo().getMethods().containsKey(sourceCodeObjectId)).
                findAny().map(documentInfoContainer -> documentInfoContainer.getMethodInfo(sourceCodeObjectId)).
                orElse(null);
    }


    public PsiFile findPsiFileByMethodId(String methodCodeObjectId) {
        return this.documents.values().stream().
                filter(documentInfoContainer -> documentInfoContainer.getDocumentInfo().getMethods().containsKey(methodCodeObjectId)).
                findAny().map(DocumentInfoContainer::getPsiFile).
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


    /**
     * getDominantLanguage should be used only if there is no better way to find the language of some code object.
     * it will return something only after at least one document was already opened. if no document was opened yet
     * it will return null.
     *
     * @return the current dominant language
     */
    @Nullable
    public Language getDominantLanguage() {

        String language = null;
        int max = 0;
        for (Map.Entry<String, MutableInt> entry : dominantLanguages.entrySet()) {
            String lang = entry.getKey();
            MutableInt mutableInt = entry.getValue();
            if (mutableInt.intValue() > max) {
                max = mutableInt.intValue();
                language = lang;
            }
        }

        return language != null ? Language.findLanguageByID(language) : null;
    }

}
