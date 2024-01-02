package org.digma.intellij.plugin.analytics;

import org.digma.intellij.plugin.model.rest.AboutResult;
import org.digma.intellij.plugin.model.rest.debugger.DebuggerEventRequest;
import org.digma.intellij.plugin.model.rest.env.DeleteEnvironmentRequest;
import org.digma.intellij.plugin.model.rest.env.DeleteEnvironmentResponse;
import org.digma.intellij.plugin.model.rest.errordetails.CodeObjectErrorDetails;
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError;
import org.digma.intellij.plugin.model.rest.event.LatestCodeObjectEventsRequest;
import org.digma.intellij.plugin.model.rest.event.LatestCodeObjectEventsResponse;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsightsStatusResponse;
import org.digma.intellij.plugin.model.rest.insights.CustomStartTimeInsightRequest;
import org.digma.intellij.plugin.model.rest.insights.InsightsOfMethodsRequest;
import org.digma.intellij.plugin.model.rest.insights.InsightsOfMethodsResponse;
import org.digma.intellij.plugin.model.rest.insights.InsightsOfSingleSpanRequest;
import org.digma.intellij.plugin.model.rest.insights.InsightsOfSingleSpanResponse;
import org.digma.intellij.plugin.model.rest.insights.InsightsRequest;
import org.digma.intellij.plugin.model.rest.insights.SpanHistogramQuery;
import org.digma.intellij.plugin.model.rest.livedata.DurationLiveData;
import org.digma.intellij.plugin.model.rest.livedata.DurationLiveDataRequest;
import org.digma.intellij.plugin.model.rest.navigation.CodeObjectNavigation;
import org.digma.intellij.plugin.model.rest.navigation.CodeObjectNavigationRequest;
import org.digma.intellij.plugin.model.rest.notifications.GetUnreadNotificationsCountRequest;
import org.digma.intellij.plugin.model.rest.notifications.NotificationsRequest;
import org.digma.intellij.plugin.model.rest.notifications.SetReadNotificationsRequest;
import org.digma.intellij.plugin.model.rest.notifications.UnreadNotificationsCountResponse;
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityRequest;
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResult;
import org.digma.intellij.plugin.model.rest.usage.EnvUsageStatusResult;
import org.digma.intellij.plugin.model.rest.usage.EnvsUsageStatusRequest;
import org.digma.intellij.plugin.model.rest.user.UserUsageStatsRequest;
import org.digma.intellij.plugin.model.rest.user.UserUsageStatsResponse;
import org.digma.intellij.plugin.model.rest.version.PerformanceMetricsResponse;
import org.digma.intellij.plugin.model.rest.version.VersionRequest;
import org.digma.intellij.plugin.model.rest.version.VersionResponse;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

public interface AnalyticsProvider extends Closeable {

    List<String> getEnvironments();

    void sendDebuggerEvent(DebuggerEventRequest debuggerEventRequest);

    List<CodeObjectInsight> getInsights(InsightsRequest insightsRequest);

    InsightsOfMethodsResponse getInsightsOfMethods(InsightsOfMethodsRequest insightsOfMethodsRequest);

    InsightsOfSingleSpanResponse getInsightsForSingleSpan(InsightsOfSingleSpanRequest insightsOfSingleSpanRequest);

    CodeObjectInsight getInsightBySpan(String environment, String spanCodeObjectId, String insightType);


    LatestCodeObjectEventsResponse getLatestEvents(LatestCodeObjectEventsRequest latestCodeObjectEventsRequest);

    List<CodeObjectError> getErrorsOfCodeObject(String environment, List<String> codeObjectIds);

    CodeObjectInsightsStatusResponse getCodeObjectInsightStatus(InsightsOfMethodsRequest request);

    void setInsightCustomStartTime(CustomStartTimeInsightRequest customStartTimeInsightRequest);

    CodeObjectErrorDetails getCodeObjectErrorDetails(String errorSourceId);

    EnvUsageStatusResult getEnvironmentsUsageStatus(EnvsUsageStatusRequest envsUsageStatusRequest);

    String getHtmlGraphForSpanPercentiles(SpanHistogramQuery request);

    String getHtmlGraphForSpanScaling(SpanHistogramQuery request);

    RecentActivityResult getRecentActivity(RecentActivityRequest recentActivityRequest);

    UserUsageStatsResponse getUserUsageStats(UserUsageStatsRequest request);

    DurationLiveData getDurationLiveData(DurationLiveDataRequest durationLiveDataRequest);

    CodeObjectNavigation getCodeObjectNavigation(CodeObjectNavigationRequest codeObjectNavigationRequest);

    String getAssetCategories(String environment, String[] services);

    String insightExists(String environment);

    String getAssets(Map<String, String> queryParams, String[] services);

    String getServices(String environment);

    String getNotifications(NotificationsRequest notificationsRequest);

    void setReadNotificationsTime(SetReadNotificationsRequest setReadNotificationsRequest);

    UnreadNotificationsCountResponse getUnreadNotificationsCount(GetUnreadNotificationsCountRequest getUnreadNotificationsCountRequest);

    VersionResponse getVersions(VersionRequest request);

    AboutResult getAbout();

    PerformanceMetricsResponse getPerformanceMetrics();

    DeleteEnvironmentResponse deleteEnvironment(DeleteEnvironmentRequest deleteEnvironmentRequest);

    // queryParams: (limit, 5), (pageSize, 5) etc
    String getDashboard(Map<String, String> queryParams);
}
