package org.digma.intellij.plugin.view;

import org.digma.intellij.plugin.ui.model.listview.ListViewItem;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds a list view items from supplied builders.
 * ListViewBuilder is not thread safe and is stateful.
 */
public abstract class ListViewBuilder {

    protected final ListGroupManager groupManager;

    protected ListViewBuilder() {
        this(new ListGroupManager());
    }

    protected ListViewBuilder(ListGroupManager groupManager) {
        this.groupManager = groupManager;
    }

    protected static List<ListViewItem<?>> sortDistinct(List<ListViewItem<?>> listViewItems) {
        return listViewItems.stream().distinct().sorted(Comparator.comparingInt(ListViewItem::getSortIndex)).collect(Collectors.toList());
    }

}
