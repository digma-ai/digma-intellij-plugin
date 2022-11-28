package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.model.rest.insights.HotspotInsight
import org.digma.intellij.plugin.ui.common.Laf
import javax.swing.JPanel


@Suppress("UNUSED_PARAMETER")
fun hotspotPanel(project: Project, modelObject: HotspotInsight): JPanel {
    return createInsightPanel(
            project = project,
            insight = modelObject,
            title = "This is an error hotspot",
            description = "Many major errors occur or propagate through this function",
            iconsList = listOf(Laf.Icons.Insight.HOTSPOT),
            bodyPanel = null,
            buttons = null,
            paginationComponent = null
    )
}
