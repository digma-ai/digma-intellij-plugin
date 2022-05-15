package org.digma.intellij.plugin.insights.view;

import org.digma.intellij.plugin.model.discovery.CodeObjectInfo;
import org.digma.intellij.plugin.model.rest.insights.ErrorInsight;
import org.digma.intellij.plugin.ui.model.insights.ErrorsListViewItem;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;
import org.digma.intellij.plugin.view.ListGroupManager;
import org.digma.intellij.plugin.view.ListViewItemBuilder;

public class ErrorsListViewItemBuilder extends ListViewItemBuilder {
    private final ErrorInsight insight;
    private final CodeObjectInfo scope;

    public ErrorsListViewItemBuilder(ErrorInsight insight, CodeObjectInfo scope) {
        this.insight = insight;
        this.scope = scope;
    }

    @Override
    public ListViewItem build(ListGroupManager groupManager) {
        ErrorsListViewItem errorsListViewItem = new ErrorsListViewItem();
        errorsListViewItem.setSortIndex(1);
        return errorsListViewItem;
    }

    @Override
    public boolean accepted() {
        return true;
    }
}
