package org.digma.intellij.plugin.insights.view;

import org.digma.intellij.plugin.model.rest.insights.SpanInsight;
import org.digma.intellij.plugin.ui.model.insights.SpanGroupListViewItem;
import org.digma.intellij.plugin.ui.model.insights.SpanListViewItem;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;
import org.digma.intellij.plugin.view.ListGroupManager;
import org.digma.intellij.plugin.view.ListViewItemBuilder;

import java.util.List;

public class SpanListViewItemBuilder implements ListViewItemBuilder<SpanInsight> {

    private static final Integer SORT_INDEX = 10;

    @Override
    public List<ListViewItem<?>> build(SpanInsight insight, ListGroupManager groupManager) {
        var span = insight.getSpan();
        var groupKey = "span:_" + span;
        SpanGroupListViewItem spanGroup = (SpanGroupListViewItem)
                groupManager.getOrCreateGroup(groupKey, () -> new SpanGroupListViewItem(span, SORT_INDEX));

        final var listViewItem = createListViewItem(insight);
        spanGroup.addItem(listViewItem);
        return List.of(spanGroup);
    }

    private ListViewItem createListViewItem(SpanInsight insight) {
        SpanListViewItem spanListViewItem = new SpanListViewItem(insight, 0);
        return spanListViewItem;
    }

}
