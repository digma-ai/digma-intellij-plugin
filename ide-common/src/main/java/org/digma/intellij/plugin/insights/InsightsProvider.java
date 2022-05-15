package org.digma.intellij.plugin.insights;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.analytics.AnalyticsProvider;
import org.digma.intellij.plugin.analytics.Environment;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.insights.view.InsightsViewBuilder;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
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


    public InsightsProvider(Project project) {
        analyticsProvider = project.getService(AnalyticsProvider.class);
        environment = project.getService(Environment.class);
        documentInfoService = project.getService(DocumentInfoService.class);
    }

    public InsightsListContainer getInsights(@NotNull MethodUnderCaret elementUnderCaret) {

        List<String> objectIds = new ArrayList<>();
        MethodInfo methodInfo = documentInfoService.getMethodInfo(elementUnderCaret);
        if (methodInfo != null) {
            objectIds.add(methodInfo.idWithType());
            objectIds.addAll(methodInfo.getRelatedCodeObjectIds());
        } else {
            objectIds.add(elementUnderCaret.idWithType());
        }

        //currently supporting only MethodUnderCaret
//        switch (elementUnderCaret.getType()) {
//            case Method: {
//
//            }
//        }


        List<CodeObjectInsight> codeObjectInsights = analyticsProvider.getInsights(new InsightsRequest(environment.getCurrent(), objectIds));
        Log.log(LOGGER::info, "CodeObjectInsights for {}: {}",methodInfo,codeObjectInsights);

        InsightsViewBuilder insightsViewBuilder = new InsightsViewBuilder(codeObjectInsights,methodInfo);
        List<ListViewItem> listViewItems = insightsViewBuilder.build();
        Log.log(LOGGER::info, "ListViewItems for {}: {}",methodInfo,listViewItems);

        return new InsightsListContainer(listViewItems,codeObjectInsights.size());

    }
}
