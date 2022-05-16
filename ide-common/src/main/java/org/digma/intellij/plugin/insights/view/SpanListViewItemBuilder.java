package org.digma.intellij.plugin.insights.view;

import org.digma.intellij.plugin.model.discovery.CodeObjectInfo;
import org.digma.intellij.plugin.model.rest.insights.SpanInsight;
import org.digma.intellij.plugin.ui.model.insights.SpanGroupListViewItem;
import org.digma.intellij.plugin.ui.model.insights.SpanListViewItem;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;
import org.digma.intellij.plugin.view.ListGroupManager;
import org.digma.intellij.plugin.view.ListViewItemBuilder;

import java.util.Collections;
import java.util.List;

public class SpanListViewItemBuilder extends ListViewItemBuilder<SpanInsight> {

    private static final Integer SORT_INDEX = 10;
    private final SpanInsight insight;
    private final CodeObjectInfo scope;

    public SpanListViewItemBuilder(SpanInsight insight, CodeObjectInfo scope) {
        super(insight);
        this.insight = insight;
        this.scope = scope;
    }

    @Override
    public List<ListViewItem> build(ListGroupManager groupManager) {
        var span = insight.getSpan();
        var groupKey = "span:_" + span;
        SpanGroupListViewItem spanGroup = (SpanGroupListViewItem)
                groupManager.getOrCreateGroup(groupKey, () -> new SpanGroupListViewItem(span,SORT_INDEX));

        ListViewItem listViewItem = createListViewItem();
        spanGroup.getItems().add(listViewItem);
        return Collections.singletonList(spanGroup);
    }

    private ListViewItem createListViewItem() {

        SpanListViewItem spanListViewItem = new SpanListViewItem(insight.getFlows(),0);
        spanListViewItem.setCodeObjectId(insight.getCodeObjectId());
        return spanListViewItem;
    }


}
