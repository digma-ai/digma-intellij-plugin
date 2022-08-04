package org.digma.intellij.plugin.analytics;

import org.digma.intellij.plugin.model.rest.errordetails.CodeObjectErrorDetails;
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.model.rest.insights.GlobalInsight;
import org.digma.intellij.plugin.model.rest.insights.InsightsRequest;
import org.digma.intellij.plugin.model.rest.insights.SpanHistogramQuery;
import org.digma.intellij.plugin.model.rest.summary.CodeObjectSummary;
import org.digma.intellij.plugin.model.rest.summary.CodeObjectSummaryRequest;
import org.digma.intellij.plugin.model.rest.usage.UsageStatusRequest;
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult;

import java.io.Closeable;
import java.util.List;

public interface AnalyticsProvider extends Closeable {

    List<String> getEnvironments();

    List<CodeObjectSummary> getSummaries(CodeObjectSummaryRequest summaryRequest);

    List<CodeObjectInsight> getInsights(InsightsRequest insightsRequest);

    List<GlobalInsight> getGlobalInsights(InsightsRequest insightsRequest);

    List<CodeObjectError> getErrorsOfCodeObject(String environment, String codeObjectId);

    CodeObjectErrorDetails getCodeObjectErrorDetails(String errorSourceId);

    UsageStatusResult getUsageStatus(UsageStatusRequest usageStatusRequest);

    String getHtmlGraphForSpanPercentiles(SpanHistogramQuery request);
}
