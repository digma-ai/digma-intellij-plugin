package org.digma.intellij.plugin.document;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.AnalyticsServiceException;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.discovery.SpanInfo;
import org.digma.intellij.plugin.model.rest.summary.CodeObjectSummary;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DocumentInfoContainer {

    private static final Logger LOGGER = Logger.getInstance(DocumentInfoContainer.class);

    private final PsiFile psiFile;
    private final AnalyticsService analyticsService;
    private DocumentInfo documentInfo;
    private Map<String, CodeObjectSummary> methodSummaries;

    public DocumentInfoContainer(@NotNull PsiFile psiFile, @NotNull AnalyticsService analyticsService) {
        this.psiFile = psiFile;
        this.analyticsService = analyticsService;
    }


    /**
     * update is invoked every time new code objects are available. usually when a document is opened in
     * the editor and every time the document changes.
     * (document change events are fired only after some quite period, see for example the grouping event in
     * Digma.Rider.Discovery.EditorListener.DocumentChangeTracker)
     */
    public void update(@NotNull DocumentInfo documentInfo) {

        Log.log(LOGGER::debug, "Updating document for {}: {}", psiFile.getVirtualFile(), documentInfo);

        //maybe documentInfo already exists, override it anyway with a new one from analysis
        this.documentInfo = documentInfo;

        loadSummaries();
    }


    private void loadSummaries() {

        List<String> objectIds = this.documentInfo.getMethods().values().stream().flatMap((Function<MethodInfo, Stream<String>>) methodInfo -> {
            var ids = new ArrayList<String>();
            ids.add(methodInfo.idWithType());
            ids.addAll(methodInfo.getSpans().stream().map(SpanInfo::idWithType).collect(Collectors.toList()));
            return ids.stream();
        }).collect(Collectors.toList());

        try {
            Log.log(LOGGER::debug, "Requesting summaries for {}: with ids {}", psiFile.getVirtualFile(), objectIds);
            List<CodeObjectSummary> summaries = analyticsService.getSummaries(objectIds);
            Log.log(LOGGER::debug, "Got summaries for {}: {}", psiFile.getVirtualFile(), summaries);
            methodSummaries = new HashMap<>();
            summaries.forEach(codeObjectSummary -> methodSummaries.put(codeObjectSummary.getCodeObjectId(), codeObjectSummary));
        } catch (AnalyticsServiceException e) {
            //methodSummaries = null means there was an error loading summaries, usually if the backend is not available.
            //don't log the exception, it was logged in AnalyticsService, keep the log quite because it can happen many times.
            methodSummaries = null;
        }
    }


    public DocumentInfo getDocumentInfo() {
        return documentInfo;
    }

    public PsiFile getPsiFile() {
        return psiFile;
    }

    public Map<String, CodeObjectSummary> getSummaries() {
        //if methodSummaries is null try to recover
        if (methodSummaries == null){
            loadSummaries();
        }
        //if methodSummaries is still null it means there is still an error loading, return an empty map to keep everything
        //working and don't crash the plugin, the next request will try to recover again
        return methodSummaries == null ? new HashMap<>() : methodSummaries;
    }



    @Nullable
    public MethodInfo getMethodInfo(String id) {
        return documentInfo.getMethods().get(id);
    }
}
