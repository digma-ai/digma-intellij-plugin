package org.digma.intellij.plugin.insights.view;

import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;
import org.digma.intellij.plugin.view.ListViewBuilder;
import org.digma.intellij.plugin.view.ListViewItemBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InsightsViewBuilder extends ListViewBuilder {

    private final BuilderHolder builderHolder;

    public InsightsViewBuilder(@NotNull BuilderHolder builderHolder) {
        this.builderHolder = Objects.requireNonNull(builderHolder, "builderHolder must not be null");
    }

    @NotNull
    public List<ListViewItem<?>> build(List<CodeObjectInsight> codeObjectInsights, @NotNull MethodInfo scope) {

        List<ListViewItem<?>> allItems = new ArrayList<>();

        codeObjectInsights.forEach(insight -> {
            final ListViewItemBuilder builder = builderHolder.getBuilder(insight.getType());
            final List<ListViewItem<?>> insightListItems = builder.build(insight, groupManager);
            allItems.addAll(insightListItems);
        });

        return sortDistinct(allItems);
    }

}
