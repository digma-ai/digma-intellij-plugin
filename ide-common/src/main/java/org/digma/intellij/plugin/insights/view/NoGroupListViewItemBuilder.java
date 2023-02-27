package org.digma.intellij.plugin.insights.view;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.ui.model.insights.InsightListViewItem;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;
import org.digma.intellij.plugin.view.ListGroupManager;
import org.digma.intellij.plugin.view.ListViewItemBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NoGroupListViewItemBuilder<T extends CodeObjectInsight> implements ListViewItemBuilder<T> {

    @Override
    public List<ListViewItem<?>> build(Project project, @NotNull MethodInfo methodInfo, T insight, ListGroupManager groupManager) {
        return List.of(new InsightListViewItem<T>(insight));
    }
}
