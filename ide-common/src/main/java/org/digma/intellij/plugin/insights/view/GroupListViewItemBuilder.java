package org.digma.intellij.plugin.insights.view;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.document.CodeObjectsUtil;
import org.digma.intellij.plugin.model.rest.insights.*;
import org.digma.intellij.plugin.ui.model.insights.InsightGroupListViewItem;
import org.digma.intellij.plugin.ui.model.insights.InsightGroupType;
import org.digma.intellij.plugin.ui.model.insights.InsightListViewItem;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;
import org.digma.intellij.plugin.view.ListGroupManager;
import org.digma.intellij.plugin.view.ListViewItemBuilder;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GroupListViewItemBuilder<T extends CodeObjectInsight> implements ListViewItemBuilder<T> {

    private final InsightGroupType insightGroupType;
    private final Function<T, String> routeFunction;
    private final Function<T, String> endpointSpan;

    public GroupListViewItemBuilder(InsightGroupType insightGroupType, Function<T, String> routeFunction,  Function<T, String> endpointSpan) {
        this.insightGroupType = insightGroupType;
        this.routeFunction = routeFunction;
        this.endpointSpan = endpointSpan;
    }

    @Override
    public List<ListViewItem<?>> build(Project project, T insight, ListGroupManager groupManager) {
        final String route = routeFunction != null ? routeFunction.apply(insight) : "";
        final String groupId = endpointSpan.apply(insight);
        final InsightGroupListViewItem theGroup = (InsightGroupListViewItem)
                groupManager.getOrCreateGroup(groupId, () -> new InsightGroupListViewItem(groupId, insightGroupType, route));

        final var theListView = new InsightListViewItem<>(insight);

        switch (insight.getType()){
            case SlowestSpans:{
                WorkspaceUrisHelper.findWorkspaceUrisForSpans(project, theListView, getSpanIds((SlowestSpansInsight) insight), insight.getCodeObjectId());
                break;
            }
            case SpanDurationBreakdown:{
                WorkspaceUrisHelper.findWorkspaceUrisForSpans(project,theListView, getSpanIds((SpanDurationBreakdownInsight) insight), insight.getCodeObjectId());
                break;
            }
            case SpanScaling:{
                WorkspaceUrisHelper.findWorkspaceUrisForSpans(project,theListView, getSpanIds((SpanScalingInsight) insight), insight.getCodeObjectId());
                break;
            }
        }

        theGroup.addItem(theListView);

        return List.of();
    }

    private List<String> getSpanIds(SpanScalingInsight insight) {
        return insight.getRootCauseSpans().stream()
                .map(it -> CodeObjectsUtil.createSpanId(it.getInstrumentationLibrary(), it.getName()))
                .collect(Collectors.toList());
    }

    private List<String> getSpanIds(SlowestSpansInsight insight) {
       return insight.getSpans().stream()
                .map(it -> CodeObjectsUtil.createSpanId(it.getSpanInfo().getInstrumentationLibrary(), it.getSpanInfo().getName()))
                .collect(Collectors.toList());
    }

    private List<String> getSpanIds(SpanDurationBreakdownInsight insight) {
        return insight.getBreakdownEntries()
                .stream().map(durationBreakdown ->
                        CodeObjectsUtil.createSpanId(durationBreakdown.getSpanInstrumentationLibrary(), durationBreakdown.getSpanName()))
                .collect(Collectors.toList());
    }

}
