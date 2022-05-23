package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.panel
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
        row{}.comment("This is an error hotspot").bold()
    }

    val iconPanel = iconPanelGrid(Icons.HOTSPOT_32, "HotSpot")
    iconPanel.border = Borders.empty(10)

    val contentPanel = JBPanel<JBPanel<*>>()
    contentPanel.layout = BorderLayout()

//    val title = JLabel("This is an error hotspot")
//    val f: Font = title.font
//    title.font = f.deriveFont(f.style or Font.BOLD)
//    title.verticalAlignment = SwingConstants.TOP
//    contentPanel.add(title, BorderLayout.NORTH)
//    val titledBorder = BorderFactory.createTitledBorder("This is an error hotspot")
//    val orgFont: Font = titledBorder.titleFont
//    val boldFont = orgFont.deriveFont(orgFont.style or Font.BOLD)
//    titledBorder.titleFont = boldFont
//    titledBorder.border = BorderFactory.createEmptyBorder()
//    contentPanel.border = titledBorder


    val message =
        JLabel("<html><b>Many major errors occur or propagate through this function.Many major errors occur or propagate through this function.</b>")
    message.horizontalAlignment = SwingConstants.LEFT

    contentPanel.add(title,BorderLayout.NORTH)
    contentPanel.add(message, BorderLayout.CENTER)


    val result = JBPanel<JBPanel<*>>()
    result.layout = BoxLayout(result, BoxLayout.X_AXIS)
    result.add(contentPanel)
    result.add(Box.createHorizontalStrut(20))
    result.add(iconPanel)

    return insightItemPanel(result)
}