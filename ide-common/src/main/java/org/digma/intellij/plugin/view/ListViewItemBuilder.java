package org.digma.intellij.plugin.view;

import org.digma.intellij.plugin.ui.model.listview.ListViewItem;

public abstract class ListViewItemBuilder {



    public abstract ListViewItem build(ListGroupManager groupManager);

    public abstract boolean accepted();
}
