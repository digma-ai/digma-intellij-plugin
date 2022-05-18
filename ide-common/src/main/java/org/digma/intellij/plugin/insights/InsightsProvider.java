package org.digma.intellij.plugin.insights;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.analytics.AnalyticsProvider;
import org.digma.intellij.plugin.analytics.Environment;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.insights.view.BuilderHolder;
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
    private final DocumentInfoService documentInfoService;

    private final BuilderHolder builderHolder = new BuilderHolder();

    public InsightsProvider(Project project) {
        analyticsProvider = project.getService(AnalyticsProvider.class);
        environment = project.getService(Environment.class);
        documentInfoService = project.getService(DocumentInfoService.class);
    }

    public InsightsListContainer getInsights(@NotNull MethodInfo methodInfo) {

        List<String> objectIds = new ArrayList<>();
        objectIds.add(methodInfo.idWithType());
        objectIds.addAll(methodInfo.getRelatedCodeObjectIds());


        List<CodeObjectInsight> codeObjectInsights = analyticsProvider.getInsights(new InsightsRequest(environment.getCurrent(), objectIds));
        Log.log(LOGGER::info, "CodeObjectInsights for {}: {}", methodInfo, codeObjectInsights);

        InsightsViewBuilder insightsViewBuilder = new InsightsViewBuilder(builderHolder);
        List<ListViewItem<?>> listViewItems = insightsViewBuilder.build(codeObjectInsights, methodInfo);
        Log.log(LOGGER::info, "ListViewItems for {}: {}", methodInfo, listViewItems);

        return new InsightsListContainer(listViewItems, codeObjectInsights.size());

    }
}
