package org.digma.intellij.plugin.errors;

import org.digma.intellij.plugin.model.rest.errors.CodeObjectError;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;

import java.util.List;

public class ErrorsListContainer {

    private final List<ListViewItem<CodeObjectError>> listViewItems;

    public ErrorsListContainer(List<ListViewItem<CodeObjectError>> listViewItems) {
        this.listViewItems = listViewItems;
    }

    public int getCount() {
        return listViewItems.size();
    }

    public List<ListViewItem<CodeObjectError>> getListViewItems() {
        return listViewItems;
    }
}
