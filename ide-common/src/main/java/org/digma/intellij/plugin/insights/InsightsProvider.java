package org.digma.intellij.plugin.insights;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.insights.view.BuildersHolder;
import org.digma.intellij.plugin.insights.view.InsightsViewBuilder;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class InsightsProvider {

    private static final Logger LOGGER = Logger.getInstance(InsightsProvider.class);

    private final AnalyticsService analyticsService;

    private final BuildersHolder buildersHolder = new BuildersHolder();

    public InsightsProvider(Project project) {
        analyticsService = project.getService(AnalyticsService.class);
    }

    public InsightsListContainer getInsights(@NotNull MethodInfo methodInfo) {

        List<String> objectIds = new ArrayList<>();
        objectIds.add(methodInfo.idWithType());
        objectIds.addAll(methodInfo.getRelatedCodeObjectIds());
        Log.log(LOGGER::debug, "Got following code object ids for method {}: {}",methodInfo,objectIds);

        List<? extends CodeObjectInsight> codeObjectInsights = analyticsService.getInsights(objectIds);
        Log.log(LOGGER::debug, "CodeObjectInsights for {}: {}", methodInfo, codeObjectInsights);

        InsightsViewBuilder insightsViewBuilder = new InsightsViewBuilder(buildersHolder);
        List<ListViewItem<?>> listViewItems = insightsViewBuilder.build(methodInfo, codeObjectInsights);
        Log.log(LOGGER::debug, "ListViewItems for {}: {}", methodInfo, listViewItems);

        return new InsightsListContainer(listViewItems, codeObjectInsights.size());

    }
}
