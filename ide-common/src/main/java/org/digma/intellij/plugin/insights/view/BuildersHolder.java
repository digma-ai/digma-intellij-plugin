package org.digma.intellij.plugin.insights.view;

import org.digma.intellij.plugin.model.InsightType;
import org.digma.intellij.plugin.model.rest.insights.ErrorInsight;
import org.digma.intellij.plugin.model.rest.insights.SlowestSpansInsight;
import org.digma.intellij.plugin.ui.model.insights.InsightGroupType;
import org.digma.intellij.plugin.view.EmptyListViewItemBuilder;
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
            case HotSpot:
                return new HotspotListViewItemBuilder();
            case Errors:
                return new NoGroupListViewItemBuilder<ErrorInsight>();
            case SpanUsages:
                return new SpanListViewItemBuilder();
            case SlowestSpans:
                return new GroupListViewItemBuilder<SlowestSpansInsight>(InsightGroupType.HttpEndpoint, slowestSpansInsight -> slowestSpansInsight.getRoute());
            case LowUsage:
            case NormalUsage:
            case HighUsage:
            case SlowEndpoint:
                return new NoGroupListViewItemBuilder<>();
            default:
                //todo: temporary until we have all types implemented
                return new EmptyListViewItemBuilder();
        }
    }

}
