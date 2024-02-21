package org.digma.intellij.plugin.insights;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.analytics.*;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.htmleditor.DigmaHTMLEditorProvider;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.InsightType;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.scope.*;
import org.digma.intellij.plugin.ui.common.Laf;
import org.digma.intellij.plugin.ui.list.insights.JaegerUtilKt;
import org.digma.intellij.plugin.ui.recentactivity.RecentActivityService;
import org.jetbrains.annotations.*;

import java.util.Collections;

public abstract class InsightsServiceImpl implements Disposable {

    private final Logger logger = Logger.getInstance(InsightsServiceImpl.class);

    private final Project project;

    public InsightsServiceImpl(Project project) {
        this.project = project;
    }


    @Override
    public void dispose() {

    }

    public void showInsight(@NotNull String spanId) {
        Log.log(logger::debug, project, "showInsight called {}", spanId);
        ScopeManager.getInstance(project).changeScope(new SpanScope(spanId));
    }


    public void openHistogram(@NotNull String instrumentationLibrary, @NotNull String spanName, @NotNull String insightType, @Nullable String displayName) {

        Log.log(logger::debug, project, "openHistogram called {},{}", instrumentationLibrary, spanName);

        ActivityMonitor.getInstance(project).registerButtonClicked("histogram", InsightType.valueOf(insightType));
        var title  = displayName != null && !displayName.isEmpty()? displayName: spanName;
        try {

            try {
                switch (InsightType.valueOf(insightType)) {
                    case SpanDurations -> {
                        String htmlContent = AnalyticsService.getInstance(project).getHtmlGraphForSpanPercentiles(instrumentationLibrary, spanName, Laf.INSTANCE.getColorHex(Laf.Colors.getPLUGIN_BACKGROUND()));
                        DigmaHTMLEditorProvider.openEditor(project, "Percentiles Graph of Span " + title, htmlContent);
                    }
                    case SpanScaling -> {
                        String htmlContent = AnalyticsService.getInstance(project).getHtmlGraphForSpanScaling(instrumentationLibrary, spanName, Laf.INSTANCE.getColorHex(Laf.Colors.getPLUGIN_BACKGROUND()));
                        DigmaHTMLEditorProvider.openEditor(project, "Scaling Graph of Span " + title, htmlContent);
                    }
                    default -> {
                        //todo: a fallback when the type is unknown, we should add support for more types if necessary
                        String htmlContent = AnalyticsService.getInstance(project).getHtmlGraphForSpanScaling(instrumentationLibrary, spanName, Laf.INSTANCE.getColorHex(Laf.Colors.getPLUGIN_BACKGROUND()));
                        DigmaHTMLEditorProvider.openEditor(project, "Scaling Graph of Span " + title, htmlContent);
                    }
                }
            } catch (IllegalArgumentException e) {
                //fallback for span type that is not in the enum
                String htmlContent = AnalyticsService.getInstance(project).getHtmlGraphForSpanScaling(instrumentationLibrary, spanName, Laf.INSTANCE.getColorHex(Laf.Colors.getPLUGIN_BACKGROUND()));
                DigmaHTMLEditorProvider.openEditor(project, "Scaling Graph of Span " + title, htmlContent);
            }

        } catch (AnalyticsServiceException e) {
            Log.warnWithException(logger, project, e, "Error in openHistogram for {},{} {}", instrumentationLibrary, title, e.getMessage());
            ErrorReporter.getInstance().reportError(project, "InsightsServiceImpl.openHistogram", e);
        }
    }


    public void openLiveView(@NotNull String prefixedCodeObjectId) {
        Log.log(logger::debug, project, "openLiveView called {}", prefixedCodeObjectId);
        project.getService(RecentActivityService.class).startLiveView(prefixedCodeObjectId);
        ActivityMonitor.getInstance(project).registerCustomEvent("live view clicked", Collections.emptyMap());
    }


    public void recalculate(@NotNull String prefixedCodeObjectId, @NotNull String insightType) {
        try {
            AnalyticsService.getInstance(project).setInsightCustomStartTime(prefixedCodeObjectId, InsightType.valueOf(insightType));
        } catch (AnalyticsServiceException e) {
            Log.warnWithException(logger, project, e, "Error in setInsightCustomStartTime {}", e.getMessage());
        }
        ActivityMonitor.getInstance(project).registerButtonClicked("recalculate", InsightType.valueOf(insightType));
    }

    public void refresh(@NotNull InsightType insightType) {
        ActivityMonitor.getInstance(project).registerButtonClicked("refresh", insightType);
    }


    public void goToTrace(@NotNull String traceId, @NotNull String traceName, @NotNull InsightType insightType, @Nullable String spanCodeObjectId) {
        JaegerUtilKt.openJaegerFromInsight(project, traceId, traceName, insightType, spanCodeObjectId);
    }

    public void goToTraceComparison(@NotNull String traceId1, @NotNull String traceName1, @NotNull String traceId2, @NotNull String traceName2, @NotNull InsightType insightType) {
        JaegerUtilKt.openJaegerComparisonFromInsight(project, traceId1, traceName1, traceId2, traceName2, insightType);
    }


}
