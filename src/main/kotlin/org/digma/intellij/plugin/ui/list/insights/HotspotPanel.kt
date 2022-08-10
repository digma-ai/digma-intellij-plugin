package org.digma.intellij.plugin.ui.list.insights

import org.digma.intellij.plugin.model.rest.insights.HotspotInsight
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import javax.swing.JPanel


@Suppress("UNUSED_PARAMETER")
fun hotspotPanel(modelObject: HotspotInsight, panelsLayoutHelper: PanelsLayoutHelper): JPanel {
    return createInsightPanel(
        "This is an error hotspot", "Many major errors occur or propagate through this function",
        Laf.Icons.Insight.HOTSPOT, "HotSpot",panelsLayoutHelper
    )
}
