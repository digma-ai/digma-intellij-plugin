package org.digma.intellij.plugin.document;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.discovery.SpanInfo;
import org.digma.intellij.plugin.model.rest.summary.CodeObjectSummary;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DocumentInfoContainer {

    private static final Logger LOGGER = Logger.getInstance(DocumentInfoContainer.class);

    private final PsiFile psiFile;
    private DocumentInfo documentInfo;
    private final Map<String, CodeObjectSummary> methodSummaries = Collections.synchronizedMap(new HashMap<>());

    public DocumentInfoContainer(@NotNull PsiFile psiFile) {
        this.psiFile = psiFile;
    }


    /**
     * update is invoked every time new code objects are available. usually when a document is opened in
     * the editor and every time the document changes.
     * (document change events are fired only after some quite period, see for example the grouping event in
     * Digma.Rider.Discovery.EditorListener.DocumentChangeTracker)
     */
    public void update(DocumentInfo documentInfo, AnalyticsService analyticsService) {

        Log.log(LOGGER::debug, "Updating document for {}: {}", psiFile.getVirtualFile(), documentInfo);

        //maybe documentInfo already exists, override it anyway with a new one from analysis
        this.documentInfo = documentInfo;

        List<String> objectIds = this.documentInfo.getMethods().values().stream().flatMap((Function<MethodInfo, Stream<String>>) methodInfo -> {
            var ids = new ArrayList<String>();
            ids.add(methodInfo.idWithType());
            ids.addAll(methodInfo.getSpans().stream().map(SpanInfo::idWithType).collect(Collectors.toList()));
            return ids.stream();
        }).collect(Collectors.toList());


        Log.log(LOGGER::debug, "Requesting summaries for {}: with ids {}", psiFile.getVirtualFile(), objectIds);
        List<CodeObjectSummary> summaries = analyticsService.getSummaries(objectIds);
        Log.log(LOGGER::debug, "Got summaries for {}: {}", psiFile.getVirtualFile(), summaries);

        //always clear methodSummaries and update with new ones
        methodSummaries.clear();
        summaries.forEach(codeObjectSummary -> methodSummaries.put(codeObjectSummary.getCodeObjectId(), codeObjectSummary));
    }


    public DocumentInfo getDocumentInfo() {
        return documentInfo;
    }

    public PsiFile getPsiFile() {
        return psiFile;
    }

    public Map<String, CodeObjectSummary> getSummaries() {
        return methodSummaries;
    }



    @Nullable
    public MethodInfo getMethodInfo(String id) {
        return documentInfo.getMethods().get(id);
    }
}
