package org.digma.intellij.plugin.view;

import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;

import java.util.List;

public class EmptyListViewItemBuilder implements ListViewItemBuilder<CodeObjectInsight> {

    @Override
    public List<ListViewItem<?>> build(CodeObjectInsight itemSource, ListGroupManager groupManager) {
        return List.of();
    }
}
