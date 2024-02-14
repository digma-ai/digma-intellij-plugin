package org.digma.intellij.plugin.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import org.digma.intellij.plugin.model.rest.AboutResult;
import org.digma.intellij.plugin.model.rest.assets.AssetDisplayInfo;
import org.digma.intellij.plugin.model.rest.codespans.CodeContextSpan;
import org.digma.intellij.plugin.model.rest.debugger.DebuggerEventRequest;
import org.digma.intellij.plugin.model.rest.env.*;
import org.digma.intellij.plugin.model.rest.errordetails.CodeObjectErrorDetails;
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError;
import org.digma.intellij.plugin.model.rest.event.*;
import org.digma.intellij.plugin.model.rest.insights.*;
import org.digma.intellij.plugin.model.rest.livedata.*;
import org.digma.intellij.plugin.model.rest.navigation.*;
import org.digma.intellij.plugin.model.rest.notifications.*;
import org.digma.intellij.plugin.model.rest.recentactivity.*;
import org.digma.intellij.plugin.model.rest.testing.LatestTestsOfSpanRequest;
import org.digma.intellij.plugin.model.rest.usage.*;
import org.digma.intellij.plugin.model.rest.user.*;
import org.digma.intellij.plugin.model.rest.version.*;

import java.io.Closeable;
import java.util.*;

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

    String getAssetCategories(Map<String, Object> queryParams);

    String getAssetFilters(Map<String, Object> queryParams);

    String insightExists(String environment);

    String getAssets(Map<String, Object> queryParams);

    String getServices(String environment);

    String getNotifications(NotificationsRequest notificationsRequest);

    void setReadNotificationsTime(SetReadNotificationsRequest setReadNotificationsRequest);

    UnreadNotificationsCountResponse getUnreadNotificationsCount(GetUnreadNotificationsCountRequest getUnreadNotificationsCountRequest);

    //Testing
    String getLatestTestsOfSpan(LatestTestsOfSpanRequest request);

    VersionResponse getVersions(VersionRequest request);

    AboutResult getAbout();

    PerformanceMetricsResponse getPerformanceMetrics();

    DeleteEnvironmentResponse deleteEnvironment(DeleteEnvironmentRequest deleteEnvironmentRequest);

    // queryParams: (limit, 5), (pageSize, 5) etc
    String getDashboard(Map<String, String> queryParams);

    LinkUnlinkTicketResponse linkTicket(LinkTicketRequest linkRequest);

    LinkUnlinkTicketResponse unlinkTicket(UnlinkTicketRequest linkRequest);

    List<CodeContextSpan> getSpansForCodeLocation(String env, List<String> idsWithType);

    AssetDisplayInfo getAssetDisplayInfo(String env, String codeObjectId);

    String getInsights(Map<String, Object> queryParams);
}
