package org.digma.intellij.plugin.insights.view;

import org.digma.intellij.plugin.model.InsightType;
import org.digma.intellij.plugin.model.rest.insights.*;
import org.digma.intellij.plugin.ui.model.insights.InsightGroupType;
import org.digma.intellij.plugin.view.ListViewItemBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class BuildersHolder {

    private final Map<InsightType, ListViewItemBuilder<?>> map = new HashMap<>(30);

    public BuildersHolder() {
        createMap();
    }

    private void createMap() {
        for (InsightType theType : InsightType.values()) {
            map.put(theType, newBuilder(theType));
        }
    }

    @NotNull
    public ListViewItemBuilder<?> getBuilder(InsightType insightType) {
        return map.get(insightType);
    }

    private ListViewItemBuilder<?> newBuilder(InsightType type) {
        switch (type) {
            //----------------------------------------------
            // Single (non grouped) Insights
            //----------------------------------------------
            case HotSpot:
                return new HotspotListViewItemBuilder();
            case Errors:
                return new SingleInsightListViewItemBuilder<ErrorInsight>();
            //----------------------------------------------
            // Spans Insights
            //----------------------------------------------
            case SpanUsages:
                return new GroupListViewItemBuilder<>(InsightGroupType.Span, SpanInsight::getSpan);
            //----------------------------------------------
            // Endpoint Insights
            //----------------------------------------------
            case SlowestSpans:
                return new GroupListViewItemBuilder<>(InsightGroupType.HttpEndpoint, SlowestSpansInsight::getRoute);
            case LowUsage:
                return new GroupListViewItemBuilder<>(InsightGroupType.HttpEndpoint, LowUsageInsight::getRoute);
            case NormalUsage:
                return new GroupListViewItemBuilder<>(InsightGroupType.HttpEndpoint, NormalUsageInsight::getRoute);
            case HighUsage:
                return new GroupListViewItemBuilder<>(InsightGroupType.HttpEndpoint, HighUsageInsight::getRoute);
            case SlowEndpoint:
                return new GroupListViewItemBuilder<>(InsightGroupType.HttpEndpoint, SlowEndpointInsight::getRoute);
            default:
                // default will fall into Single (non grouped) reference
                return new SingleInsightListViewItemBuilder<>();
        }
    }

}
