package org.digma.intellij.plugin.insights.view;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
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

        theGroup.addItem(theListView);

        return List.of();
    }



}
