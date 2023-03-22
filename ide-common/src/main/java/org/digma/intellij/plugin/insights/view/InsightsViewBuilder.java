package org.digma.intellij.plugin.insights.view;

import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.discovery.SpanInfo;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.model.rest.insights.EndpointInsight;
import org.digma.intellij.plugin.model.rest.insights.EndpointSchema;
import org.digma.intellij.plugin.model.rest.insights.SpanInsight;
import org.digma.intellij.plugin.ui.model.insights.InsightGroupListViewItem;
import org.digma.intellij.plugin.ui.model.insights.InsightGroupType;
import org.digma.intellij.plugin.ui.model.insights.NoDataYet;
import org.digma.intellij.plugin.ui.model.listview.GroupListViewItem;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;
import org.digma.intellij.plugin.view.ListViewBuilder;
import org.digma.intellij.plugin.view.ListViewItemBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class InsightsViewBuilder extends ListViewBuilder {

    private final BuildersHolder buildersHolder;

    public InsightsViewBuilder(@NotNull BuildersHolder buildersHolder) {
        this.buildersHolder = Objects.requireNonNull(buildersHolder, "buildersHolder must not be null");
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public List<ListViewItem<?>> build(Project project, @NotNull MethodInfo methodInfo, List<? extends CodeObjectInsight> codeObjectInsights) {

        adjustToHttpIfNeeded(codeObjectInsights);

        List<ListViewItem<?>> allItems = new ArrayList<>();

        codeObjectInsights.forEach(insight -> {
            final ListViewItemBuilder<CodeObjectInsight> builder = (ListViewItemBuilder<CodeObjectInsight>) buildersHolder.getBuilder(insight.getType());
            final List<ListViewItem<?>> insightListItems = builder.build(project, methodInfo, insight, groupManager);
            allItems.addAll(insightListItems);
        });

        Set<String> spansThatHaveNoInsight = findLocalSpansThatHaveNoInsights(methodInfo, codeObjectInsights);
        if (!spansThatHaveNoInsight.isEmpty()) {
            buildItemsForNoDataYet(spansThatHaveNoInsight);
        }

        allItems.addAll(groupManager.getGroupItems());

        return sortDistinct(allItems);
    }

    protected static Set<String> findLocalSpansThatHaveNoInsights(@NotNull MethodInfo methodInfo, List<? extends CodeObjectInsight> codeObjectInsights) {

        Set<String> spansOfMethod = methodInfo.getSpans().stream()
                .map(SpanInfo::getName)
                .collect(Collectors.toSet());

        Set<String> spansThatHaveInsights = codeObjectInsights.stream()
                .filter(it -> it instanceof SpanInsight)
                .map(it -> (SpanInsight) it)
                .map(SpanInsight::spanName)// span name without instrumentation library
                .collect(Collectors.toSet());

        // spansOfMethod minus spansThatHaveInsights
        Set<String> spansThatHaveNoInsights = Sets.difference(spansOfMethod, spansThatHaveInsights).immutableCopy();

        return spansThatHaveNoInsights;
    }

    protected void buildItemsForNoDataYet(Set<String> spansThatHaveNoInsight) {
        for (String currSpanName : spansThatHaveNoInsight) {
            String groupId = currSpanName;
            GroupListViewItem theGroup = groupManager.getOrCreateGroup(
                    groupId, () -> new InsightGroupListViewItem(currSpanName, InsightGroupType.Span, ""));

            ListViewItem<NoDataYet> itemOfNoDataYet = new ListViewItem<>(new NoDataYet(), 222);
            theGroup.addItem(itemOfNoDataYet);
        }
    }

    protected void adjustToHttpIfNeeded(final List<? extends CodeObjectInsight> codeObjectInsights) {
        codeObjectInsights
                .stream()
                .filter(coi -> coi instanceof EndpointInsight)
                .map(coi -> (EndpointInsight) coi)
                .forEach(EndpointSchema::adjustHttpRouteIfNeeded);
    }

}
