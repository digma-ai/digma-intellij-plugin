package org.digma.intellij.plugin.insights.view;

import org.digma.intellij.plugin.model.discovery.CodeObjectInfo;
import org.digma.intellij.plugin.model.rest.insights.ErrorInsight;
import org.digma.intellij.plugin.ui.model.insights.ErrorsListViewItem;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;
import org.digma.intellij.plugin.view.ListGroupManager;
import org.digma.intellij.plugin.view.ListViewItemBuilder;

import java.util.Collections;
import java.util.List;

public class ErrorsListViewItemBuilder extends ListViewItemBuilder<ErrorInsight> {
    private final ErrorInsight insight;
    private final CodeObjectInfo scope;

    public ErrorsListViewItemBuilder(ErrorInsight insight, CodeObjectInfo scope) {
        super(insight);
        this.insight = insight;
        this.scope = scope;
    }

    @Override
    public List<ListViewItem> build(ListGroupManager groupManager) {
        ErrorsListViewItem errorsListViewItem = new ErrorsListViewItem("",1);
        return Collections.singletonList(errorsListViewItem);
    }

}
