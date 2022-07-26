package org.digma.intellij.plugin.insights;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.AnalyticsServiceException;
import org.digma.intellij.plugin.insights.view.BuildersHolder;
import org.digma.intellij.plugin.insights.view.InsightsViewBuilder;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.InsightType;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class InsightsProvider {

    private static final Logger LOGGER = Logger.getInstance(InsightsProvider.class);

    private final AnalyticsService analyticsService;
    private final Project project;

    private final BuildersHolder buildersHolder = new BuildersHolder();

    public InsightsProvider(Project project) {
        analyticsService = project.getService(AnalyticsService.class);
        this.project = project;
    }

    public InsightsListContainer getInsights(@NotNull MethodInfo methodInfo) {

        List<String> objectIds = new ArrayList<>();
        objectIds.add(methodInfo.idWithType());
        objectIds.addAll(methodInfo.getRelatedCodeObjectIds());
        Log.log(LOGGER::debug, "Got following code object ids for method {}: {}",methodInfo.getId(),objectIds);

        try {
            List<? extends CodeObjectInsight> codeObjectInsights = analyticsService.getInsights(objectIds);
            codeObjectInsights = filterUnmapped(codeObjectInsights);
            Log.log(LOGGER::debug, "CodeObjectInsights for {}: {}", methodInfo.getId(), codeObjectInsights);
            final UsageStatusResult usageStatus = analyticsService.getUsageStatus(objectIds);
            InsightsViewBuilder insightsViewBuilder = new InsightsViewBuilder(buildersHolder);
            List<ListViewItem<?>> listViewItems = insightsViewBuilder.build(project,methodInfo, codeObjectInsights);
            Log.log(LOGGER::debug, "ListViewItems for {}: {}", methodInfo.getId(), listViewItems);
            return new InsightsListContainer(listViewItems, codeObjectInsights.size(), usageStatus);
        }catch (AnalyticsServiceException e){
            //if analyticsService.getInsights throws exception it means insights could not be loaded, usually when
            //the backend is not available. return an empty InsightsListContainer to keep everything running and don't
            //crash the plugin. don't log the exception, it was logged in AnalyticsService, keep the log quite because
            //it may happen many times.
            Log.log(LOGGER::debug, "AnalyticsServiceException for getInsights for {}: {}", methodInfo.getId(), e.getMessage());
            return new InsightsListContainer();
        }
    }

    private List<? extends CodeObjectInsight> filterUnmapped(List<? extends CodeObjectInsight> codeObjectInsights) {
        var filteredInsights = new ArrayList<CodeObjectInsight>();
        codeObjectInsights.forEach(codeObjectInsight -> {
            if (!codeObjectInsight.getType().equals(InsightType.Unmapped)){
                filteredInsights.add(codeObjectInsight);
            }
        });
        return filteredInsights;
    }
}
