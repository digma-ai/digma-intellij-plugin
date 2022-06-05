package org.digma.intellij.plugin.insights;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.analytics.AnalyticsProvider;
import org.digma.intellij.plugin.analytics.Environment;
import org.digma.intellij.plugin.insights.view.BuildersHolder;
import org.digma.intellij.plugin.insights.view.InsightsViewBuilder;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.model.rest.insights.InsightsRequest;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class InsightsProvider {

    private static final Logger LOGGER = Logger.getInstance(InsightsProvider.class);

    private final AnalyticsProvider analyticsProvider;
    private final Environment environment;

    private final BuildersHolder buildersHolder = new BuildersHolder();

    public InsightsProvider(Project project) {
        analyticsProvider = project.getService(AnalyticsProvider.class);
        environment = project.getService(Environment.class);
    }

    public InsightsListContainer getInsights(@NotNull MethodInfo methodInfo) {

        List<String> objectIds = new ArrayList<>();
        objectIds.add(methodInfo.idWithType());
        objectIds.addAll(methodInfo.getRelatedCodeObjectIds());
        Log.log(LOGGER::debug, "Got {} code object ids for method {}",objectIds.size(), methodInfo.getId());

        List<? extends CodeObjectInsight> codeObjectInsights = analyticsProvider.getInsights(new InsightsRequest(environment.getCurrent(), objectIds));
        Log.log(LOGGER::debug, "CodeObjectInsights for {}: {}", methodInfo, codeObjectInsights);

        InsightsViewBuilder insightsViewBuilder = new InsightsViewBuilder(buildersHolder);
        List<ListViewItem<?>> listViewItems = insightsViewBuilder.build(methodInfo, codeObjectInsights);
        Log.log(LOGGER::debug, "ListViewItems for {}: {}", methodInfo, listViewItems);

        return new InsightsListContainer(listViewItems, codeObjectInsights.size());

    }
}
