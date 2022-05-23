package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.util.ui.JBUI.Borders
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.HotspotInsight
import org.digma.intellij.plugin.ui.common.iconPanelGrid
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.BorderLayout
import javax.swing.*


fun hotspotPanel(listViewItem: ListViewItem<HotspotInsight>): JPanel {

    val hotspotInsight: HotspotInsight = listViewItem.modelObject

    val title = panel{
        row{
            label("This is an error hotspot")
                .bold()
                .verticalAlign(VerticalAlign.TOP)
        }
    }
    title.border = Borders.empty()


//    val title = JLabel("This is an error hotspot")
//    val f: Font = title.font
//    title.font = f.deriveFont(f.style or Font.BOLD)
    val message =
        JLabel("<html>Many major errors occur or propagate through this function.")
    message.horizontalAlignment = SwingConstants.LEFT

    val contentPanel = JBPanel<JBPanel<*>>()
    contentPanel.layout = BorderLayout(0,0)
    contentPanel.add(title,BorderLayout.NORTH)
    contentPanel.add(message, BorderLayout.CENTER)

    val iconPanel = iconPanelGrid(Icons.HOTSPOT_24, "HotSpot")
    iconPanel.border = Borders.empty()

    val result = JBPanel<JBPanel<*>>()
    result.layout = BoxLayout(result, BoxLayout.X_AXIS)
    result.add(contentPanel)
    result.add(Box.createHorizontalStrut(5))
    result.add(iconPanel)

    return insightItemPanel(result)
}