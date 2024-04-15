package org.digma.intellij.plugin.analytics;

import org.digma.intellij.plugin.model.rest.AboutResult;
import org.digma.intellij.plugin.model.rest.assets.AssetDisplayInfo;
import org.digma.intellij.plugin.model.rest.codelens.*;
import org.digma.intellij.plugin.model.rest.codespans.CodeContextSpans;
import org.digma.intellij.plugin.model.rest.common.SpanHistogramQuery;
import org.digma.intellij.plugin.model.rest.debugger.DebuggerEventRequest;
import org.digma.intellij.plugin.model.rest.env.*;
import org.digma.intellij.plugin.model.rest.environment.Env;
import org.digma.intellij.plugin.model.rest.errordetails.CodeObjectErrorDetails;
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError;
import org.digma.intellij.plugin.model.rest.event.*;
import org.digma.intellij.plugin.model.rest.highlights.HighlightsPerformanceResponse;
import org.digma.intellij.plugin.model.rest.insights.*;
import org.digma.intellij.plugin.model.rest.livedata.*;
import org.digma.intellij.plugin.model.rest.login.*;
import org.digma.intellij.plugin.model.rest.lowlevel.*;
import org.digma.intellij.plugin.model.rest.navigation.*;
import org.digma.intellij.plugin.model.rest.notifications.*;
import org.digma.intellij.plugin.model.rest.recentactivity.*;
import org.digma.intellij.plugin.model.rest.tests.LatestTestsOfSpanRequest;
import org.digma.intellij.plugin.model.rest.user.*;
import org.digma.intellij.plugin.model.rest.version.*;

import java.io.Closeable;
import java.util.*;

public interface AnalyticsProvider extends Closeable {

    LoginResponse login(LoginRequest loginRequest);

    LoginResponse refreshToken(RefreshRequest loginRequest);


    List<Env> getEnvironments();

    void sendDebuggerEvent(DebuggerEventRequest debuggerEventRequest);

    List<InsightTypesForJaegerResponse> getInsightsForJaeger(InsightTypesForJaegerRequest request);

    String getInsightBySpan(String environment, String spanCodeObjectId, String insightType);

    LatestCodeObjectEventsResponse getLatestEvents(LatestCodeObjectEventsRequest latestCodeObjectEventsRequest);

    List<CodeObjectError> getErrorsOfCodeObject(String environment, List<String> codeObjectIds);

    void setInsightCustomStartTime(CustomStartTimeInsightRequest customStartTimeInsightRequest);

    CodeObjectErrorDetails getCodeObjectErrorDetails(String errorSourceId);

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

    Optional<LoadStatusResponse> getLoadStatus();

    DeleteEnvironmentResponse deleteEnvironment(DeleteEnvironmentRequest deleteEnvironmentRequest);

    // queryParams: (limit, 5), (pageSize, 5) etc
    String getDashboard(Map<String, String> queryParams);

    LinkUnlinkTicketResponse linkTicket(LinkTicketRequest linkRequest);

    LinkUnlinkTicketResponse unlinkTicket(UnlinkTicketRequest linkRequest);

    CodeContextSpans getSpansForCodeLocation(String env, List<String> idsWithType);

    CodeLensOfMethodsResponse getCodeLensByMethods(CodeLensOfMethodsRequest codeLensOfMethodsRequest);

    AssetDisplayInfo getAssetDisplayInfo(String env, String codeObjectId);

    String getInsights(Map<String, Object> queryParams);

    AssetNavigationResponse getAssetNavigation(String env, String spanCodeObjectId);

    String createEnvironments(Map<String, Object> request);

    void deleteEnvironmentV2(String id);

    void markInsightsAsRead(List<String> insightIds);
    void markAllInsightsAsRead(String environment, MarkInsightsAsReadScope scope);

    void dismissInsight(String  insightId);

    void undismissInsight(String insightId);

    InsightsStatsResult getInsightsStats(Map<String, Object> queryParams);

    HttpResponse lowLevelCall(HttpRequest request);

    List<HighlightsPerformanceResponse> getHighlightsPerformance(Map<String, Object> queryParams);

    String getHighlightsTopInsights(Map<String, Object> queryParams);
}
