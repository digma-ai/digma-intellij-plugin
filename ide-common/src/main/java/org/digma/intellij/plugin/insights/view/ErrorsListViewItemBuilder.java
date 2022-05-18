package org.digma.intellij.plugin.insights.view;

import org.digma.intellij.plugin.model.rest.insights.ErrorInsight;
import org.digma.intellij.plugin.ui.model.insights.ErrorsListViewItem;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;
import org.digma.intellij.plugin.view.ListGroupManager;
import org.digma.intellij.plugin.view.ListViewItemBuilder;

import java.util.List;

public class ErrorsListViewItemBuilder implements ListViewItemBuilder<ErrorInsight> {

    @Override
    public List<ListViewItem<?>> build(ErrorInsight insight, ListGroupManager groupManager) {
        ErrorsListViewItem errorsListViewItem = new ErrorsListViewItem(insight, 1);
        return List.of(errorsListViewItem);
    }

}
