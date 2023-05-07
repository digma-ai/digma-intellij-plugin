package org.digma.intellij.plugin.document;

import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiFile;
import kotlin.Pair;
import org.apache.commons.collections4.CollectionUtils;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.AnalyticsServiceException;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.InsightType;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.model.rest.insights.InsightsOfMethodsResponse;
import org.digma.intellij.plugin.model.rest.insights.MethodWithInsights;
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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

    // map between methodId to its insights
    @NotNull
    private ConcurrentMap<String, List<CodeObjectInsight>> insightsMap = new ConcurrentHashMap();

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
        List<MethodInfo> methodInfos = getMethodInfos();
        try {
            Log.log(LOGGER::debug, "Requesting insights by methodInfos {}", methodInfos);
            InsightsOfMethodsResponse response = analyticsService.getInsightsOfMethods(methodInfos);

            insightsMap = createMapOfInsights(response); // replace the existing map with a new one

            Log.log(LOGGER::debug, "Got insights for {}: {}", psiFile.getVirtualFile(), insightsMap.values());
        } catch (AnalyticsServiceException e) {
            //insights = Collections.emptyList() means there was an error loading insights, usually if the backend is not available.
            //don't log the exception, it was logged in AnalyticsService, keep the log quite because it can happen many times.
            insightsMap = new ConcurrentHashMap<>();
            Log.log(LOGGER::warn, "Cannot get insights by methodInfos: {}. Because: {}", methodInfos, e.getMessage());
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
            Log.log(LOGGER::warn, "Cannot get usage status with ids: {}. Because: {}", objectIds, e.getMessage());
        }
    }

    // returns true if data has changed or not
    public boolean loadInsightsForMethod(String methodId) {
        MethodInfo methodInfo = getMethodInfo(methodId);
        if (methodInfo == null) {
            return false;
        }

        InsightsOfMethodsResponse insightsOfMethods;

        try {
            insightsOfMethods = analyticsService.getInsightsOfMethods(List.of(methodInfo));
        } catch (AnalyticsServiceException e) {
            Log.log(LOGGER::warn, "Cannot get insights by methodInfo: {}. Because: {}", methodInfo, e.getMessage());
            return false;
        }

        if (CollectionUtils.isEmpty(insightsOfMethods.getMethodsWithInsights())) {
            Log.log(LOGGER::warn, "Cannot get insights by methodInfo: {}. Because size of MethodsWithInsights is 0 while expected to be at least 1", methodInfo);
            return false;
        }

        MethodWithInsights mwi = insightsOfMethods.getMethodsWithInsights().iterator().next();
        Pair<String, List<CodeObjectInsight>> kvPair = createKeyAndValue(mwi);
        List<CodeObjectInsight> prevInsights;
        List<CodeObjectInsight> newInsights;

        if (kvPair != null) {
            newInsights = kvPair.getSecond();
            prevInsights = insightsMap.put(kvPair.getFirst(), newInsights);
        } else {
            newInsights = Collections.emptyList();
            prevInsights = insightsMap.remove(methodId);
        }

        if (prevInsights == null) {
            prevInsights = Collections.emptyList();
        }

        return !newInsights.equals(prevInsights);
    }

    private static ConcurrentMap<String, List<CodeObjectInsight>> createMapOfInsights(InsightsOfMethodsResponse response) {
        ConcurrentMap<String, List<CodeObjectInsight>> result = new ConcurrentHashMap<>();
        response.getMethodsWithInsights()
                .forEach(methodWithInsights -> {
                    Pair<String, List<CodeObjectInsight>> kvPair = createKeyAndValue(methodWithInsights);
                    if (kvPair != null) {
                        result.put(kvPair.getFirst(), kvPair.getSecond());
                    }
                });
        return result;
    }

    @Nullable
    private static Pair<String, List<CodeObjectInsight>> createKeyAndValue(MethodWithInsights methodWithInsights) {
        var insights = methodWithInsights.getInsights()
                .stream()
                .filter(CodeObjectInsight::isTypeMapped)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(insights)) {
            return null;
        }

        var methodId = MethodInfo.removeType(methodWithInsights.getMethodWithIds().getCodeObjectId()); // without type
        return new Pair(methodId, insights);
    }

    private List<String> getObjectIdsForCurrentDocument() {
        return this.documentInfo.getMethods().values().stream().flatMap((Function<MethodInfo, Stream<String>>) methodInfo -> {
            var ids = new ArrayList<String>();
            ids.addAll(methodInfo.allIdsWithType());
            ids.addAll(methodInfo.getRelatedCodeObjectIdsWithType());
            return ids.stream();
        }).collect(Collectors.toList());
    }

    private List<MethodInfo> getMethodInfos() {
        return new ArrayList<>(this.documentInfo.getMethods().values());
    }


    public DocumentInfo getDocumentInfo() {
        return documentInfo;
    }

    public PsiFile getPsiFile() {
        return psiFile;
    }

    public int getInsightsCount() {
        return (int) insightsMap.values()
                .stream()
                .mapToLong(List::size)
                .sum();
    }

    public int countInsightsByType(InsightType insightType) {
        return (int) insightsMap.values()
                .stream()
                .flatMap(List::stream)
                .filter(it -> insightType.equals(it.getType()))
                .count();
    }

    @NotNull
    public List<CodeObjectInsight> getInsightsForMethod(String methodId) {
        var retVal = insightsMap.getOrDefault(methodId, Collections.emptyList());
        return retVal;
    }

    public Map<String, List<CodeObjectInsight>> getAllMethodWithInsightsMapForCurrentDocument() {
        return insightsMap;
    }

    public boolean hasInsights(String methodId) {
        return insightsMap.containsKey(methodId);
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
