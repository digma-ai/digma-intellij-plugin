package org.digma.intellij.plugin.insights;

import org.digma.intellij.plugin.ui.model.listview.ListViewItem;

import java.util.List;

public class InsightsListContainer {

    private int count = 0;
    private List<ListViewItem> listViewItems;

    public InsightsListContainer(List<ListViewItem> listViewItems, int count) {
        this.listViewItems = listViewItems;
        this.count = count;
    }


    public int getCount() {
        return count;
    }

    public List<ListViewItem> getListViewItems() {
        return listViewItems;
    }
}
