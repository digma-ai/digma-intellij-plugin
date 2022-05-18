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

    private final BuildersHolder buildersHolder;

    public InsightsViewBuilder(@NotNull BuildersHolder buildersHolder) {
        this.buildersHolder = Objects.requireNonNull(buildersHolder, "buildersHolder must not be null");
    }

    @NotNull
    public List<ListViewItem<?>> build(@NotNull MethodInfo scope, List<CodeObjectInsight> codeObjectInsights) {

        List<ListViewItem<?>> allItems = new ArrayList<>();

        codeObjectInsights.forEach(insight -> {
            final ListViewItemBuilder builder = buildersHolder.getBuilder(insight.getType());
            final List<ListViewItem<?>> insightListItems = builder.build(insight, groupManager);
            allItems.addAll(insightListItems);
        });

        return sortDistinct(allItems);
    }

}
