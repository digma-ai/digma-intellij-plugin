package org.digma.intellij.plugin.insights.view;

import org.digma.intellij.plugin.model.InsightType;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.model.rest.insights.EPNPlusSpansInsight;
import org.digma.intellij.plugin.model.rest.insights.EndpointBreakdownInsight;
import org.digma.intellij.plugin.model.rest.insights.EndpointDurationSlowdownInsight;
import org.digma.intellij.plugin.model.rest.insights.ErrorInsight;
import org.digma.intellij.plugin.model.rest.insights.HighUsageInsight;
import org.digma.intellij.plugin.model.rest.insights.HotspotInsight;
import org.digma.intellij.plugin.model.rest.insights.LowUsageInsight;
import org.digma.intellij.plugin.model.rest.insights.NormalUsageInsight;
import org.digma.intellij.plugin.model.rest.insights.SlowEndpointInsight;
import org.digma.intellij.plugin.model.rest.insights.SlowestSpansInsight;
import org.digma.intellij.plugin.model.rest.insights.SpanDurationBreakdownInsight;
import org.digma.intellij.plugin.model.rest.insights.SpanDurationsInsight;
import org.digma.intellij.plugin.model.rest.insights.SpanNPlusOneInsight;
import org.digma.intellij.plugin.model.rest.insights.SpanScalingInsight;
import org.digma.intellij.plugin.model.rest.insights.SpanSlowEndpointsInsight;
import org.digma.intellij.plugin.model.rest.insights.SpanUsagesInsight;
import org.digma.intellij.plugin.model.rest.insights.UnmappedInsight;
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
                return new GroupListViewItemBuilder<SpanUsagesInsight>(InsightGroupType.Span, null, SpanUsagesInsight::spanName);
            case SpanDurations:
                return new GroupListViewItemBuilder<SpanDurationsInsight>(InsightGroupType.Span, null, spanDurationsInsight -> spanDurationsInsight.spanName());
            case SpanScaling:
                return new GroupListViewItemBuilder<SpanScalingInsight>(InsightGroupType.Span, null, SpanScalingInsight::spanName);
            case SpanDurationBreakdown:
                return new GroupListViewItemBuilder<SpanDurationBreakdownInsight>(InsightGroupType.Span, null, SpanDurationBreakdownInsight::spanName);
            case SpanEndpointBottleneck:
                return new GroupListViewItemBuilder<SpanSlowEndpointsInsight>(InsightGroupType.Span, null, insight -> insight.spanName());
            case SpaNPlusOne:
                return new GroupListViewItemBuilder<SpanNPlusOneInsight>(InsightGroupType.Span, null, insight -> insight.spanName());
            case SlowestSpans:
                return new GroupListViewItemBuilder<SlowestSpansInsight>(InsightGroupType.HttpEndpoint, SlowestSpansInsight::getRoute, SlowestSpansInsight::endpointSpanName);
            case LowUsage:
                return new GroupListViewItemBuilder<LowUsageInsight>(InsightGroupType.HttpEndpoint, LowUsageInsight::getRoute, LowUsageInsight::endpointSpanName);
            case NormalUsage:
                return new GroupListViewItemBuilder<NormalUsageInsight>(InsightGroupType.HttpEndpoint, NormalUsageInsight::getRoute, NormalUsageInsight::endpointSpanName);
            case HighUsage:
                return new GroupListViewItemBuilder<HighUsageInsight>(InsightGroupType.HttpEndpoint, HighUsageInsight::getRoute, HighUsageInsight::endpointSpanName);
            case SlowEndpoint:
                return new GroupListViewItemBuilder<SlowEndpointInsight>(InsightGroupType.HttpEndpoint, SlowEndpointInsight::getRoute, SlowEndpointInsight::endpointSpanName);
            case EndpointSpaNPlusOne:
                return new GroupListViewItemBuilder<EPNPlusSpansInsight>(InsightGroupType.HttpEndpoint, EPNPlusSpansInsight::getRoute, EPNPlusSpansInsight::endpointSpanName);
            case EndpointDurationSlowdown:
                return new GroupListViewItemBuilder<EndpointDurationSlowdownInsight>(InsightGroupType.HttpEndpoint, EndpointDurationSlowdownInsight::getRoute, EndpointDurationSlowdownInsight::endpointSpanName);
            case EndpointBreakdown:
                return new GroupListViewItemBuilder<EndpointBreakdownInsight>(InsightGroupType.HttpEndpoint, EndpointBreakdownInsight::getRoute, EndpointBreakdownInsight::endpointSpanName);
            case Unmapped:
                return new NoGroupListViewItemBuilder<UnmappedInsight>();
            default:
                //todo: temporary until we have all types implemented
                return new EmptyListViewItemBuilder();
        }
    }

}
