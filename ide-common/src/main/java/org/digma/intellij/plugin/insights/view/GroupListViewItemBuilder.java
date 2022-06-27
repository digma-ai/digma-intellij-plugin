package org.digma.intellij.plugin.insights.view;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.model.rest.insights.SlowestSpansInsight;
import org.digma.intellij.plugin.ui.model.insights.InsightGroupListViewItem;
import org.digma.intellij.plugin.ui.model.insights.InsightGroupType;
import org.digma.intellij.plugin.ui.model.insights.InsightListViewItem;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;
import org.digma.intellij.plugin.view.ListGroupManager;
import org.digma.intellij.plugin.view.ListViewItemBuilder;

import java.util.List;
import java.util.function.Function;

public class GroupListViewItemBuilder<T extends CodeObjectInsight> implements ListViewItemBuilder<T> {

    private final InsightGroupType insightGroupType;
    private final Function<T, String> groupByFunction;

    public GroupListViewItemBuilder(InsightGroupType insightGroupType, Function<T, String> groupByFunction) {
        this.insightGroupType = insightGroupType;
        this.groupByFunction = groupByFunction;
    }

    @Override
    public List<ListViewItem<?>> build(Project project, T insight, ListGroupManager groupManager) {
        final String groupId = groupByFunction.apply(insight);
        final String groupKey = insightGroupType.name() + ":" + groupId;
        final InsightGroupListViewItem theGroup = (InsightGroupListViewItem)
                groupManager.getOrCreateGroup(groupKey, () -> new InsightGroupListViewItem(groupId, insightGroupType));

        final var theListView = new InsightListViewItem<>(insight);

        switch (insight.getType()){
            case SlowestSpans:{
                SlowestSpansHelper.findWorkspaceUrisForSpans(project,theListView, (SlowestSpansInsight) insight);
                break;
            }
        }

        theGroup.addItem(theListView);

        return List.of();
    }




}
