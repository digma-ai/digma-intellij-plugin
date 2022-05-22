package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.HotspotInsight
import org.digma.intellij.plugin.ui.common.iconPanel
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants


fun hotspotPanel(listViewItem: ListViewItem<HotspotInsight>): JPanel {

    val hotspotInsight: HotspotInsight = listViewItem.modelObject

    val iconPanel = iconPanel(Icons.HOTSPOT_32, "HotSpot")

    val result = JBPanel<JBPanel<*>>()
    result.layout = BoxLayout(result, BoxLayout.X_AXIS)

    val contentPanel = JBPanel<JBPanel<*>>()
    contentPanel.layout = BorderLayout()

//    val title = JLabel("This is an error hotspot")
//    val f: Font = title.font
//    title.font = f.deriveFont(f.style or Font.BOLD)
//    title.verticalAlignment = SwingConstants.TOP
//    contentPanel.add(title, BorderLayout.NORTH)
    val titledBorder = BorderFactory.createTitledBorder("This is an error hotspot")
    val orgFont: Font = titledBorder.titleFont
    val boldFont = orgFont.deriveFont(orgFont.style or Font.BOLD)
    titledBorder.titleFont = boldFont
    titledBorder.border = BorderFactory.createEmptyBorder()
    contentPanel.border = titledBorder


    val message =
        JLabel("<html>Many major errors occur or propagate through this function.Many major errors occur or propagate through this function.")
    message.horizontalAlignment = SwingConstants.LEFT
    contentPanel.add(message, BorderLayout.CENTER)

    result.add(contentPanel)
    result.add(iconPanel)
    return result
}