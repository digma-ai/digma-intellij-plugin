package org.digma.intellij.plugin.view;

import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;

import java.util.List;

/**
 * ListViewItemBuilder is initialized with a CodeObjectInsight and on build will build a
 * ListViewItem from it.
 * Usually builders will build one ListViewItem, but it can return a list of ListViewItem in case a
 * ListViewItem needs to belong to two groups or maybe one insight creates two UI elements.
 *
 * @param <T>
 */
public abstract class ListViewItemBuilder<T extends CodeObjectInsight> {

    private T itemSource;

    public ListViewItemBuilder(T itemSource) {
        this.itemSource = itemSource;
    }

    /**
     * build one or more ListViewItem from the insight.
     * if a builder adds the ListViewItem to a group it should return the group and not the
     * ListViewItem. the ListViewItems returned here are considered 'top level items' in the UI list, it may contain
     * groups of items.
     * @param groupManager manages group instances.
     * @return one or more ListViewItems
     */
    public abstract List<ListViewItem> build(ListGroupManager groupManager);


}
