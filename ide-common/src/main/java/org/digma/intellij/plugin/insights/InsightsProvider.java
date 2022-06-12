package org.digma.intellij.plugin.insights;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.AnalyticsServiceException;
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

        try {
            List<? extends CodeObjectInsight> codeObjectInsights = analyticsService.getInsights(objectIds);
            Log.log(LOGGER::debug, "CodeObjectInsights for {}: {}", methodInfo, codeObjectInsights);
            InsightsViewBuilder insightsViewBuilder = new InsightsViewBuilder(buildersHolder);
            List<ListViewItem<?>> listViewItems = insightsViewBuilder.build(methodInfo, codeObjectInsights);
            Log.log(LOGGER::debug, "ListViewItems for {}: {}", methodInfo, listViewItems);
            return new InsightsListContainer(listViewItems, codeObjectInsights.size());
        }catch (AnalyticsServiceException e){
            //if analyticsService.getInsights throws exception it means insights could not be loaded, usually when
            //the backend is not available. return an empty InsightsListContainer to keep everything running and don't
            //crash the plugin. don't log the exception, it was logged in AnalyticsService, keep the log quite because
            //it may happen many times.
            return new InsightsListContainer(new ArrayList<>(), 0);
        }
    }
}
