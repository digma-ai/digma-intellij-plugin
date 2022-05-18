package org.digma.intellij.plugin.view;

import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;

import java.util.List;

/**
 * Usually builders will build one ListViewItem, but it can return a list of ListViewItem in case a
 * ListViewItem needs to belong to two groups or maybe one insight creates two UI elements.
 *
 * @param <T> CodeObjectInsight
 */
public interface ListViewItemBuilder<T extends CodeObjectInsight> {

    /**
     * build one or more ListViewItem from the insight.
     * if a builder adds the ListViewItem to a group it should return the group and not the
     * ListViewItem. the ListViewItems returned here are considered 'top level items' in the UI list, it may contain
     * groups of items.
     *
     * @param codeObjectInsight self explanatory.
     * @param groupManager      manages group instances.
     * @return one or more ListViewItems
     */
    List<ListViewItem<?>> build(T codeObjectInsight, ListGroupManager groupManager);


}
