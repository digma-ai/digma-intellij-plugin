package org.digma.intellij.plugin.insights;

import org.digma.intellij.plugin.ui.model.listview.ListViewItem;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class InsightsListContainer {

    private final int count;
    private final List<ListViewItem<?>> listViewItems;

    public InsightsListContainer(List<ListViewItem<?>> listViewItems, int count) {
        this.listViewItems = listViewItems;
        this.count = count;
    }


    public int getCount() {
        return count;
    }

    @Nullable
    public List<ListViewItem<?>> getListViewItems() {
        return listViewItems;
    }

}
