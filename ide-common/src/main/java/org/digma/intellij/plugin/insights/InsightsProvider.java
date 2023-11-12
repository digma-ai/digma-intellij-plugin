package org.digma.intellij.plugin.insights;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang3.time.StopWatch;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.AnalyticsServiceException;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.insights.view.BuildersHolder;
import org.digma.intellij.plugin.insights.view.InsightsViewBuilder;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.InsightType;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsightsStatusResponse;
import org.digma.intellij.plugin.model.rest.insights.InsightStatus;
import org.digma.intellij.plugin.model.rest.insights.MethodWithInsightStatus;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class InsightsProvider {

    private static final Logger LOGGER = Logger.getInstance(InsightsProvider.class);

    private final AnalyticsService analyticsService;
    private final DocumentInfoService documentInfoService;
    private final Project project;

    private final BuildersHolder buildersHolder = new BuildersHolder();

    public InsightsProvider(Project project) {
        analyticsService = project.getService(AnalyticsService.class);
        documentInfoService = project.getService(DocumentInfoService.class);
        this.project = project;
    }

    public InsightsListContainer getCachedInsights(@NotNull MethodInfo methodInfo) {
        List<? extends CodeObjectInsight> cachedMethodInsights = documentInfoService.getCachedMethodInsights(methodInfo);
        return getInsightsListContainer(methodInfo, cachedMethodInsights);
    }

    public InsightsListContainer getInsightsListContainer(@NotNull MethodInfo methodInfo, List<? extends CodeObjectInsight> insightsList) {
        List<String> objectIds = getObjectIdsWithType(methodInfo);
        Log.log(LOGGER::debug, "Got following code object ids for method {}: {}", methodInfo.getId(), objectIds);
        var stopWatch = StopWatch.createStarted();

        try {
            List<? extends CodeObjectInsight> codeObjectInsights = insightsList;
            codeObjectInsights = filterUnmapped(codeObjectInsights);
            Log.log(LOGGER::debug, "CodeObjectInsights for {}: {}", methodInfo.getId(), codeObjectInsights);
            InsightsViewBuilder insightsViewBuilder = new InsightsViewBuilder(buildersHolder);
            List<ListViewItem<?>> listViewItems = insightsViewBuilder.build(project, methodInfo, codeObjectInsights);
            Log.log(LOGGER::trace, "ListViewItems for {}: {}", methodInfo.getId(), listViewItems);
            return new InsightsListContainer(listViewItems, codeObjectInsights.size());
        } finally {
            stopWatch.stop();
            Log.log(LOGGER::debug, "getUsageStatus time took {} milliseconds", stopWatch.getTime(TimeUnit.MILLISECONDS));
        }
    }

    @Nullable
    public InsightStatus getInsightStatus(@NotNull MethodInfo methodInfo) {
        try {
            CodeObjectInsightsStatusResponse response = analyticsService.getCodeObjectInsightStatus(List.of(methodInfo));
            MethodWithInsightStatus methodResp = response.getCodeObjectsWithInsightsStatus().stream().findFirst().orElse(null);
            return methodResp == null ? null : methodResp.getInsightStatus();
        } catch (AnalyticsServiceException e) {
            Log.log(LOGGER::debug, "AnalyticsServiceException for getCodeObjectInsightStatus for {}: {}", methodInfo.getId(), e.getMessage());
            return null;
        }
    }

    private List<String> getObjectIdsWithType(@NotNull MethodInfo methodInfo) {
        List<String> objectIds = new ArrayList<>();
        objectIds.addAll(methodInfo.allIdsWithType());
        objectIds.addAll(methodInfo.getRelatedCodeObjectIdsWithType());
        return objectIds;
    }

    private List<? extends CodeObjectInsight> filterUnmapped(List<? extends CodeObjectInsight> codeObjectInsights) {
        var filteredInsights = new ArrayList<CodeObjectInsight>();
        codeObjectInsights.forEach(codeObjectInsight -> {
            if (!codeObjectInsight.getType().equals(InsightType.Unmapped)) {
                filteredInsights.add(codeObjectInsight);
            }
        });
        return filteredInsights;
    }
}
