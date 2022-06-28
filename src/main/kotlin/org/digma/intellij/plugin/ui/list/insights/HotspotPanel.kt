package org.digma.intellij.plugin.ui.list.insights

import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.HotspotInsight
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import javax.swing.JPanel


fun hotspotPanel(listViewItem: ListViewItem<HotspotInsight>): JPanel {
    return createInsightPanel(
        "This is an error hotspot", "Many major errors occur or propagate through this function",
        Icons.Insight.HOTSPOT, "HotSpot"
    )
}
