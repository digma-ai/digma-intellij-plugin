package org.digma.intellij.plugin.insights.view;

import org.digma.intellij.plugin.model.rest.insights.HotspotInsight;
import org.digma.intellij.plugin.ui.model.insights.HotspotListViewItem;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;
import org.digma.intellij.plugin.view.ListGroupManager;
import org.digma.intellij.plugin.view.ListViewItemBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HotspotListViewItemBuilder implements ListViewItemBuilder<HotspotInsight> {

    @Override
    public List<ListViewItem<?>> build(HotspotInsight insight, @NotNull ListGroupManager groupManager) {


        HotspotListViewItem hotspotListViewItem = new HotspotListViewItem(insight,
                "This is an error hotspot",
                "Many major errors occur or propagate through this function.",
                "See how this was calculated",
                "https://phmecloud.blob.core.windows.net/photo/web/ou0ehpjndrfhkkx1tekojx0-3.png"
        );

        return List.of(hotspotListViewItem);
    }

}
