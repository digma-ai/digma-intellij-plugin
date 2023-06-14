package org.digma.intellij.plugin.analytics;

import org.digma.intellij.plugin.model.rest.AboutResult;
import org.digma.intellij.plugin.model.rest.debugger.DebuggerEventRequest;
import org.digma.intellij.plugin.model.rest.errordetails.CodeObjectErrorDetails;
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsightsStatusResponse;
import org.digma.intellij.plugin.model.rest.insights.CustomStartTimeInsightRequest;
import org.digma.intellij.plugin.model.rest.insights.GlobalInsight;
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
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityRequest;
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResult;
import org.digma.intellij.plugin.model.rest.usage.UsageStatusRequest;
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult;
import org.digma.intellij.plugin.model.rest.version.VersionRequest;
import org.digma.intellij.plugin.model.rest.version.VersionResponse;

import java.io.Closeable;
import java.util.List;

public interface AnalyticsProvider extends Closeable {

    List<String> getEnvironments();

    void sendDebuggerEvent(DebuggerEventRequest debuggerEventRequest);

    /**
     * @deprecated This method is deprecated and will be removed in a future release.
     * Use {@link #getInsightsOfMethods(InsightsOfMethodsRequest insightsOfMethodsRequest)} instead.
     */
    @Deprecated
    List<CodeObjectInsight> getInsights(InsightsRequest insightsRequest);

    InsightsOfMethodsResponse getInsightsOfMethods(InsightsOfMethodsRequest insightsOfMethodsRequest);

    InsightsOfSingleSpanResponse getInsightsForSingleSpan(InsightsOfSingleSpanRequest insightsOfSingleSpanRequest);

    List<GlobalInsight> getGlobalInsights(InsightsRequest insightsRequest);

    List<CodeObjectError> getErrorsOfCodeObject(String environment, List<String> codeObjectIds);

    CodeObjectInsightsStatusResponse getCodeObjectInsightStatus(InsightsOfMethodsRequest request);

    void setInsightCustomStartTime(CustomStartTimeInsightRequest customStartTimeInsightRequest);

    CodeObjectErrorDetails getCodeObjectErrorDetails(String errorSourceId);

    UsageStatusResult getUsageStatus(UsageStatusRequest usageStatusRequest);

    String getHtmlGraphForSpanPercentiles(SpanHistogramQuery request);

    String getHtmlGraphForSpanScaling(SpanHistogramQuery request);

    RecentActivityResult getRecentActivity(RecentActivityRequest recentActivityRequest);

    DurationLiveData getDurationLiveData(DurationLiveDataRequest durationLiveDataRequest);

    CodeObjectNavigation getCodeObjectNavigation(CodeObjectNavigationRequest codeObjectNavigationRequest);

    VersionResponse getVersions(VersionRequest request);

    AboutResult getAbout();
}
