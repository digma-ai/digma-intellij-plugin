package org.digma.intellij.plugin.insights.view;

import org.digma.intellij.plugin.model.discovery.CodeObjectInfo;
import org.digma.intellij.plugin.model.rest.insights.HotspotInsight;
import org.digma.intellij.plugin.ui.model.insights.HotspotListViewItem;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;
import org.digma.intellij.plugin.view.ListGroupManager;
import org.digma.intellij.plugin.view.ListViewItemBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class HotspotListViewItemBuilder extends ListViewItemBuilder<HotspotInsight> {


    private static final Integer SORT_INDEX = 0;

    private final HotspotInsight insight;
    @SuppressWarnings("FieldCanBeLocal")
    private final CodeObjectInfo scope;

    public HotspotListViewItemBuilder(@NotNull HotspotInsight insight, CodeObjectInfo scope) {
        super(insight);
        this.insight = insight;
        this.scope = scope;
    }

    @Override
    public List<ListViewItem> build(@NotNull ListGroupManager groupManager) {


        HotspotListViewItem hotspotListViewItem = new HotspotListViewItem(
                "This is an error hotspot",
                "Many major errors occur or propagate through this function.",
                "See how this was calculated",
                "https://phmecloud.blob.core.windows.net/photo/web/ou0ehpjndrfhkkx1tekojx0-3.png",
                SORT_INDEX);

        hotspotListViewItem.setCodeObjectId(insight.getCodeObjectId());

        return Collections.singletonList(hotspotListViewItem);
    }

}
