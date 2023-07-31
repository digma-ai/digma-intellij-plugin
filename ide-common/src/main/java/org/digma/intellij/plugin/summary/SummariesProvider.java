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

public class SummariesProvider {

    private static final Logger LOGGER = Logger.getInstance(SummariesProvider.class);

    private final AnalyticsService analyticsService;

    public SummariesProvider(Project project) {
        analyticsService = project.getService(AnalyticsService.class);
    }

    public List<ListViewItem<GlobalInsight>> getGlobalInsights() {

        try {
            List<GlobalInsight> globalInsights = analyticsService.getGlobalInsights();
            Log.log(LOGGER::trace, "GlobalInsights: {}", globalInsights);
            return toListViewItems(globalInsights);
        } catch (AnalyticsServiceException e) {
            //if analyticsService.getGlobalInsights throws exception it means insights could not be loaded, usually when
            //the backend is not available. return an empty List<GlobalInsight> to keep everything running and don't
            //crash the plugin. don't log the exception, it was logged in AnalyticsService, keep the log quite because
            //it may happen many times.
            Log.warnWithException(LOGGER, e, "AnalyticsServiceException for getGlobalInsights: {}", e.getMessage());
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
            Log.warnWithException(LOGGER, e, "AnalyticsServiceException for getGlobalInsights: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<ListViewItem<GlobalInsight>> toListViewItems(List<GlobalInsight> insights) {
        List<ListViewItem<GlobalInsight>> items = new ArrayList<>();

        for (GlobalInsight insight : insights) {
            ListViewItem<GlobalInsight> item = new ListViewItem<>(insight, 1);

            items.add(item);
        }

        return items;
    }

}
