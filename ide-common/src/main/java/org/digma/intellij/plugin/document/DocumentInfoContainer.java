package org.digma.intellij.plugin.document;

import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.AnalyticsServiceException;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.InsightType;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.digma.intellij.plugin.model.Models.Empties.EmptyUsageStatusResult;

/**
 * A container for one document info, it holds the discovery info and the info from the analytics service.
 */
public class DocumentInfoContainer {

    private final Logger LOGGER = Logger.getInstance(DocumentInfoContainer.class);

    private final PsiFile psiFile;

    private final Language language;

    private final AnalyticsService analyticsService;
    private DocumentInfo documentInfo;

    @NotNull
    private List<CodeObjectInsight> insights = new ArrayList<>();
    private UsageStatusResult usageStatus = EmptyUsageStatusResult;
    private UsageStatusResult usageStatusOfErrors = EmptyUsageStatusResult;

    public DocumentInfoContainer(@NotNull PsiFile psiFile, @NotNull AnalyticsService analyticsService) {
        this.psiFile = psiFile;
        this.analyticsService = analyticsService;
        language = psiFile.getLanguage();
    }

    public Language getLanguage() {
        return language;
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

        loadAllInsightsForCurrentDocument();
    }

    public void updateCache() {
        Log.log(LOGGER::debug, "Refreshing document backend data for {}: ", psiFile.getVirtualFile());
        loadAllInsightsForCurrentDocument();
    }

    private void loadAllInsightsForCurrentDocument() {
        List<String> objectIds = getObjectIdsForCurrentDocument();
        try {
            Log.log(LOGGER::debug, "Requesting insights with ids {}", objectIds);
            insights = analyticsService.getInsights(objectIds)
                    .stream().filter(codeObjectInsight -> !codeObjectInsight.getType().equals(InsightType.Unmapped))
                    .collect(Collectors.toList());
            Log.log(LOGGER::debug, "Got next insights: {}", insights);
        } catch (AnalyticsServiceException e) {
            //insights = Collections.emptyList() means there was an error loading insights, usually if the backend is not available.
            //don't log the exception, it was logged in AnalyticsService, keep the log quite because it can happen many times.
            insights = Collections.emptyList();
            Log.log(LOGGER::debug, "Cannot get insights with ids: {}. Because: {}", objectIds, e.getMessage());
        }

        try {
            Log.log(LOGGER::debug, "Requesting usage status for {}: with ids {}", psiFile.getVirtualFile(), objectIds);
            usageStatus = analyticsService.getUsageStatus(objectIds);
            Log.log(LOGGER::debug, "Got usage status for {}: {}", psiFile.getVirtualFile(), usageStatus);

            Log.log(LOGGER::debug, "Requesting usage status of errors for {}: with ids {}", psiFile.getVirtualFile(), objectIds);
            usageStatusOfErrors = analyticsService.getUsageStatusOfErrors(objectIds);
            Log.log(LOGGER::debug, "Got usage status of errors for {}: {}", psiFile.getVirtualFile(), usageStatusOfErrors);
        } catch (AnalyticsServiceException e) {
            usageStatus = EmptyUsageStatusResult;
            usageStatusOfErrors = EmptyUsageStatusResult;
        }
    }

    private List<String> getObjectIdsForCurrentDocument() {
        return this.documentInfo.getMethods().values().stream().flatMap((Function<MethodInfo, Stream<String>>) methodInfo -> {
            var ids = new ArrayList<String>();
            ids.add(methodInfo.idWithType());
            ids.addAll(methodInfo.getRelatedCodeObjectIdsWithType());
            return ids.stream();
        }).collect(Collectors.toList());
    }


    public DocumentInfo getDocumentInfo() {
        return documentInfo;
    }

    public PsiFile getPsiFile() {
        return psiFile;
    }

    @NotNull
    public List<CodeObjectInsight> getAllInsights() {
        return insights;
    }

    public UsageStatusResult getUsageStatus() {
        return usageStatus;
    }

    public UsageStatusResult getUsageStatusOfErrors() {
        return usageStatusOfErrors;
    }

    @Nullable
    public MethodInfo getMethodInfo(String id) {
        return documentInfo.getMethods().get(id);
    }

}
