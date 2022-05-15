package org.digma.intellij.plugin.insights.view;

import org.digma.intellij.plugin.model.InsightType;
import org.digma.intellij.plugin.model.discovery.CodeObjectInfo;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.model.rest.insights.ErrorInsight;
import org.digma.intellij.plugin.model.rest.insights.HotspotInsight;
import org.digma.intellij.plugin.model.rest.insights.SpanInsight;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;
import org.digma.intellij.plugin.view.ListViewBuilder;
import org.digma.intellij.plugin.view.ListViewItemBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class InsightsViewBuilder extends ListViewBuilder {


    private final List<CodeObjectInsight> codeObjectInsights;
    private final CodeObjectInfo scope;

    public InsightsViewBuilder(@NotNull List<CodeObjectInsight> codeObjectInsights, MethodInfo scope) {
        this.codeObjectInsights = codeObjectInsights;
        this.scope = scope;
    }

    @NotNull
    public List<ListViewItem> build() {

        List<ListViewItemBuilder> itemBuilders = new ArrayList<>();

        codeObjectInsights.forEach(insight -> {
            InsightType type = insight.getType();
            ListViewItemBuilder builder = newBuilder(type, insight);
            itemBuilders.add(builder);
        });

        return build(itemBuilders);

    }

    private ListViewItemBuilder newBuilder(InsightType type, CodeObjectInsight insight) {

        switch (type) {
            case HotSpot:
                return new HotspotListViewItemBuilder((HotspotInsight) insight,scope);
            case Errors:
                return new ErrorsListViewItemBuilder((ErrorInsight) insight,scope);
            case SpanUsages:
                return new SpanListViewItemBuilder((SpanInsight) insight,scope);
            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }
    }
}
