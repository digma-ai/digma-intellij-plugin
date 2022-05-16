package org.digma.intellij.plugin.view;

import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds a list view items from supplied builders.
 * ListViewBuilder is not thread safe and is stateful.
 */
public abstract class ListViewBuilder<T extends CodeObjectInsight> {

    private final ListGroupManager groupManager;

    public ListViewBuilder() {
        this(new ListGroupManager());
    }

    public ListViewBuilder(ListGroupManager groupManager) {
        this.groupManager = groupManager;
    }



    protected List<ListViewItem> build(List<ListViewItemBuilder<T>> itemBuilders){

        List<ListViewItem> listViewItems = new ArrayList<>();

        itemBuilders.forEach(builder -> {
            listViewItems.addAll(builder.build(groupManager));
        });

        return sortDistinct(listViewItems);
    }

    private List<ListViewItem> sortDistinct(List<ListViewItem> listViewItems) {
        return listViewItems.stream().distinct().sorted(Comparator.comparingInt(ListViewItem::getSortIndex)).collect(Collectors.toList());
    }


}
