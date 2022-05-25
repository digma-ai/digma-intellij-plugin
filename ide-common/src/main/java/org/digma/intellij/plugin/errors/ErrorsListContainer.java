package org.digma.intellij.plugin.errors;

import org.digma.intellij.plugin.ui.model.listview.ListViewItem;

import java.util.ArrayList;
import java.util.List;

public class ErrorsListContainer {
    public ErrorsListContainer(ArrayList<ListViewItem<?>> listViewItems, int count) {

    }


    public int getCount() {
        return 1;
    }

    public List<ListViewItem<?>> getListViewItems() {
        var dummyList = new ArrayList<ListViewItem<?>>();
        dummyList.add(new ListViewItem<>("dummy",0){

        });
        return dummyList;
    }
}
