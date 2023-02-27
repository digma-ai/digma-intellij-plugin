package org.digma.intellij.plugin.view;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EmptyListViewItemBuilder implements ListViewItemBuilder<CodeObjectInsight> {

    @Override
    public List<ListViewItem<?>> build(Project project, @NotNull MethodInfo methodInfo, CodeObjectInsight itemSource, ListGroupManager groupManager) {
        return List.of();
    }
}
