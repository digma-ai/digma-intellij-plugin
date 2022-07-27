package org.digma.intellij.plugin.summary;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.AnalyticsServiceException;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.rest.insights.GlobalInsight;
import org.digma.intellij.plugin.model.rest.usage.EnvironmentUsageStatus;
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SummariesProvider {

    private static final Logger LOGGER = Logger.getInstance(SummariesProvider.class);

    private final AnalyticsService analyticsService;
    private final Project project;

    public SummariesProvider(Project project) {
        analyticsService = project.getService(AnalyticsService.class);
        this.project = project;
    }

    public List<ListViewItem<GlobalInsight>> getGlobalInsights() {

        try {
            List<GlobalInsight> globalInsights = analyticsService.getGlobalInsights();
            Log.log(LOGGER::debug, "GlobalInsights: {}", globalInsights);
            return globalInsights.stream()
                    .map(x -> new ListViewItem<>(x, 1))
                    .collect(Collectors.toList());
        } catch (AnalyticsServiceException e) {
            //if analyticsService.getGlobalInsights throws exception it means insights could not be loaded, usually when
            //the backend is not available. return an empty List<GlobalInsight> to keep everything running and don't
            //crash the plugin. don't log the exception, it was logged in AnalyticsService, keep the log quite because
            //it may happen many times.
            Log.log(LOGGER::debug, "AnalyticsServiceException for getGlobalInsights: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<EnvironmentUsageStatus> getEnvironmentStatuses() {
        try {
            final UsageStatusResult usageStatus = analyticsService.getUsageStatus(Collections.emptyList());
            return usageStatus != null
                    ? usageStatus.getEnvironmentStatuses()
                    : Collections.emptyList();
        } catch (AnalyticsServiceException e) {
            Log.log(LOGGER::debug, "AnalyticsServiceException for getGlobalInsights: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<ListViewItem<?>> toListViewItems(List<GlobalInsight> insights) {
        List<ListViewItem<?>> items = new ArrayList<>();


//        insights.forEach(insight -> {
//            if (insight instanceof TopErrorFlowsInsight) {
//                GroupListViewItem group = new GroupListViewItem(0, insight.getType().toString());
//                int index = 0;
//                ((TopErrorFlowsInsight) insight).getErrors().forEach(error -> {
//                    group.addItem(new ListViewItem(error, index++));
//                });
//                items.add(group);
//            } else if (insight instanceof SpanDurationChangeInsight) {
//                GroupListViewItem group = new GroupListViewItem(1, insight.getType().toString());
//                ((SpanDurationChangeInsight) insight).getSpanDurationChanges().forEach(change -> {
//                    group.addItem(new ListViewItem(change, 0));
//                });
//                items.add(group);
//            } else {
//                Log.log(LOGGER::warn, "Unknown global : {}", insight.getType());
//            }
//        });

        return items;
    }
}
