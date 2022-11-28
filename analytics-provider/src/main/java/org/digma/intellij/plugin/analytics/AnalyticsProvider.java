package org.digma.intellij.plugin.analytics;

import org.digma.intellij.plugin.model.rest.debugger.DebuggerEventRequest;
import org.digma.intellij.plugin.model.rest.errordetails.CodeObjectErrorDetails;
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError;
import org.digma.intellij.plugin.model.rest.insights.*;
import org.digma.intellij.plugin.model.rest.summary.CodeObjectSummary;
import org.digma.intellij.plugin.model.rest.summary.CodeObjectSummaryRequest;
import org.digma.intellij.plugin.model.rest.usage.UsageStatusRequest;
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult;

import java.io.Closeable;
import java.util.List;

public interface AnalyticsProvider extends Closeable {

    List<String> getEnvironments();

    void sendDebuggerEvent(DebuggerEventRequest debuggerEventRequest);

    List<CodeObjectSummary> getSummaries(CodeObjectSummaryRequest summaryRequest);

    List<CodeObjectInsight> getInsights(InsightsRequest insightsRequest);

    List<GlobalInsight> getGlobalInsights(InsightsRequest insightsRequest);

    List<CodeObjectError> getErrorsOfCodeObject(String environment, String codeObjectId);

    void setInsightCustomStartTime(CustomStartTimeInsightRequest customStartTimeInsightRequest);

    CodeObjectErrorDetails getCodeObjectErrorDetails(String errorSourceId);

    UsageStatusResult getUsageStatus(UsageStatusRequest usageStatusRequest);

    String getHtmlGraphForSpanPercentiles(SpanHistogramQuery request);
}
