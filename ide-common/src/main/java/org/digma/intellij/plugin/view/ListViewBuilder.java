package org.digma.intellij.plugin.view;

import org.digma.intellij.plugin.ui.model.listview.ListViewItem;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds a list view from supplied builders.
 * ListViewBuilder is not thread safe and is stateful.
 */
public abstract class ListViewBuilder {

    private final ListGroupManager groupManager;

    public ListViewBuilder() {
        this(new ListGroupManager());
    }

    public ListViewBuilder(ListGroupManager groupManager) {
        this.groupManager = groupManager;
    }



    protected List<ListViewItem> build(List<ListViewItemBuilder> itemBuilders){

        List<ListViewItem> listViewItems = new ArrayList<>();

        itemBuilders.forEach(builder -> {
            if (builder.accepted()) {
                listViewItems.add(builder.build(groupManager));
            }
        });

        return sortDistinct(listViewItems);
    }

    private List<ListViewItem> sortDistinct(List<ListViewItem> listViewItems) {
        return listViewItems.stream().distinct().sorted(Comparator.comparingInt(ListViewItem::getSortIndex)).collect(Collectors.toList());
    }


}
