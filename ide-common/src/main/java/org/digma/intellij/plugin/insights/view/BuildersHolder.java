package org.digma.intellij.plugin.insights.view;

import org.digma.intellij.plugin.model.InsightType;
import org.digma.intellij.plugin.model.rest.insights.*;
import org.digma.intellij.plugin.ui.model.insights.InsightGroupType;
import org.digma.intellij.plugin.view.EmptyListViewItemBuilder;
import org.digma.intellij.plugin.view.ListViewItemBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class BuildersHolder {

    private final Map<InsightType, ListViewItemBuilder<? extends CodeObjectInsight>> map = new HashMap<>(30);

    public BuildersHolder() {
        createMap();
    }

    private void createMap() {
        for (InsightType theType : InsightType.values()) {
            map.put(theType, newBuilder(theType));
        }
    }

    @NotNull
    public ListViewItemBuilder<? extends CodeObjectInsight> getBuilder(InsightType insightType) {
        return map.get(insightType);
    }

    private ListViewItemBuilder<? extends CodeObjectInsight> newBuilder(InsightType type) {
        switch (type) {
            case HotSpot:
                return new NoGroupListViewItemBuilder<HotspotInsight>();
            case Errors:
                return new NoGroupListViewItemBuilder<ErrorInsight>();
            case SpanUsages:
                return new GroupListViewItemBuilder<SpanUsagesInsight>(InsightGroupType.Span, SpanUsagesInsight::getSpan);
            case SpanDurations:
                return new GroupListViewItemBuilder<SpanDurationsInsight>(InsightGroupType.Span, spanDurationsInsight -> spanDurationsInsight.getSpan().getName());
            case SpanDurationBreakdown:
                return new GroupListViewItemBuilder<SpanDurationBreakdownInsight>(InsightGroupType.Span, SpanDurationBreakdownInsight::getSpanName);
            case SpanEndpointBottleneck:
                return new GroupListViewItemBuilder<SpanSlowEndpointsInsight>(InsightGroupType.Span, insight -> insight.getSpan().getName());
            case SlowestSpans:
                return new GroupListViewItemBuilder<SlowestSpansInsight>(InsightGroupType.HttpEndpoint, SlowestSpansInsight::getRoute);
            case LowUsage:
                return new GroupListViewItemBuilder<LowUsageInsight>(InsightGroupType.HttpEndpoint, LowUsageInsight::getRoute);
            case NormalUsage:
                return new GroupListViewItemBuilder<NormalUsageInsight>(InsightGroupType.HttpEndpoint, NormalUsageInsight::getRoute);
            case HighUsage:
                return new GroupListViewItemBuilder<HighUsageInsight>(InsightGroupType.HttpEndpoint, HighUsageInsight::getRoute);
            case SlowEndpoint:
                return new GroupListViewItemBuilder<SlowEndpointInsight>(InsightGroupType.HttpEndpoint, SlowEndpointInsight::getRoute);
            case Unmapped:
                return new NoGroupListViewItemBuilder<UnmappedInsight>();
            default:
                //todo: temporary until we have all types implemented
                return new EmptyListViewItemBuilder();
        }
    }

}
