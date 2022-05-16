package org.digma.intellij.plugin.view;

import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;

import java.util.ArrayList;
import java.util.List;

public class EmptyListViewItemBuilder extends ListViewItemBuilder{

    public EmptyListViewItemBuilder(CodeObjectInsight itemSource) {
        super(itemSource);
    }

    @Override
    public List<ListViewItem> build(ListGroupManager groupManager) {
        return new ArrayList<>();
    }
}
