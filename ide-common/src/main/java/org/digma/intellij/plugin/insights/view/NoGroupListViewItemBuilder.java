package org.digma.intellij.plugin.insights.view;

import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.ui.model.insights.InsightListViewItem;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;
import org.digma.intellij.plugin.view.ListGroupManager;
import org.digma.intellij.plugin.view.ListViewItemBuilder;

import java.util.List;

public class NoGroupListViewItemBuilder<T extends CodeObjectInsight> implements ListViewItemBuilder<T> {

    @Override
    public List<ListViewItem<?>> build(T insight, ListGroupManager groupManager) {
        return List.of(new InsightListViewItem<T>(insight));
    }
}
