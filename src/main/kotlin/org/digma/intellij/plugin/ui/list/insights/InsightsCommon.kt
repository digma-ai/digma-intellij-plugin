package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.model.rest.insights.UnmappedInsight
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.buildBoldTitleGrayedComment
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import org.digma.intellij.plugin.ui.list.listGroupPanel
import org.digma.intellij.plugin.ui.list.listItemPanel
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.*
import javax.swing.*
import kotlin.math.max


fun insightItemPanel(panel: JPanel): JPanel {
    return listItemPanel(panel)
}

fun insightGroupPanel(panel: JPanel): JPanel {
    return listGroupPanel(panel)
}


fun createInsightPanel(title: String, body: String, icon: Icon, iconText: String, panelsLayoutHelper: PanelsLayoutHelper): JPanel {
    return createInsightPanel(title, body, icon, iconText, true,panelsLayoutHelper)
}

fun createInsightPanel(title: String, body: String, icon: Icon, iconText: String, wrap: Boolean, panelsLayoutHelper: PanelsLayoutHelper): JPanel {

    val iconPanel = insightsIconPanelBorder(icon, iconText, panelsLayoutHelper)
    iconPanel.isOpaque = false

    val message = JLabel(buildBoldTitleGrayedComment(title,body),SwingConstants.LEFT)
    val messagePanel = JBPanel<JBPanel<*>>()
    messagePanel.layout = BorderLayout()
    messagePanel.add(message,BorderLayout.NORTH)
    messagePanel.border = empty()
    messagePanel.isOpaque = false

    val result = JBPanel<JBPanel<*>>()

    result.layout = BorderLayout()
    result.add(messagePanel,BorderLayout.CENTER)
    result.add(iconPanel,BorderLayout.EAST)

    return if (wrap) {
        insightItemPanel(result)
    } else {
        result
    }
}


fun unmappedInsightPanel(listViewItem: ListViewItem<UnmappedInsight>, panelsLayoutHelper: PanelsLayoutHelper): JPanel {

    val methodName = listViewItem.modelObject.codeObjectId.substringAfterLast("\$_\$")
    return createInsightPanel("Unmapped insight: '${listViewItem.modelObject.theType}'",
        "unmapped insight type for '$methodName'",
        Icons.QUESTION_MARK, "",panelsLayoutHelper)
}


fun genericPanelForSingleInsight(listViewItem: ListViewItem<CodeObjectInsight>, panelsLayoutHelper: PanelsLayoutHelper): JPanel {

    return createInsightPanel("Generic insight panel",
        "Insight named ${listViewItem.modelObject.javaClass.simpleName}",
        Icons.QUESTION_MARK, "",panelsLayoutHelper)
}





internal fun insightsIconPanelBorder(icon: Icon, text: String, panelsLayoutHelper: PanelsLayoutHelper): JPanel {


    val panel: JPanel = object: JPanel(){
        override fun getPreferredSize(): Dimension {
            val ps = super.getPreferredSize()
            if (ps == null){
                return ps
            }
//            if (isVisible) {
                val h = ps.height
                val w = ps.width
                addCurrentLargestWidthIconPanel(panelsLayoutHelper,w)
                return Dimension(getCurrentLargestWidthIconPanel(panelsLayoutHelper,w), h)
//            }else{
//                return ps
//            }
        }
    }

    panel.layout = BorderLayout()

    val iconLabel = JLabel(icon)
    iconLabel.horizontalAlignment = SwingConstants.CENTER
    panel.add(iconLabel, BorderLayout.CENTER)

    if (text.isNotBlank()) {
        val textLabel = JLabel(text)
        textLabel.horizontalAlignment = SwingConstants.CENTER
        panel.add(textLabel, BorderLayout.SOUTH)
    }

    panel.border = empty(2,0,0,Laf.scaleBorders(getInsightIconPanelRightBorderSize()))
    panel.isOpaque = false

    val width = panel.preferredSize.width
    addCurrentLargestWidthIconPanel(panelsLayoutHelper,width)

    return panel
}




internal fun getInsightIconPanelRightBorderSize():Int{
    return 5
}
internal fun getCurrentLargestWidthIconPanel(layoutHelper: PanelsLayoutHelper, width: Int):Int{
    //this method should never return null and never throw NPE
    val currentLargest: Int =
        (layoutHelper.getObjectAttribute("insightsIconPanelBorder","largestWidth")?: 0) as Int
    return max(width,currentLargest)
}
internal fun addCurrentLargestWidthIconPanel(layoutHelper: PanelsLayoutHelper,width: Int){
    //this method should never throw NPE
    val currentLargest: Int =
        (layoutHelper.getObjectAttribute("insightsIconPanelBorder","largestWidth")?: 0) as Int
    layoutHelper.addObjectAttribute("insightsIconPanelBorder","largestWidth",
        max(currentLargest,width))
}



internal fun panelOfUnsupported(caption: String): JPanel {
    return panel {
        row("Unsupported yet: '$caption'") {
        }.layout(RowLayout.PARENT_GRID)
    }
}


//fun iconPanelBox(icon: Icon, text: String): JPanel {
//    val panel = JPanel()
//    panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
//    val iconLabel = JLabel(icon)
//    panel.add(iconLabel)
//    val label = JLabel(text)
//    panel.add(label)
//    panel.border = empty()
//    return panel
//}
//
//fun iconPanelGrid(icon: Icon, text: String): JPanel {
//    val panel = JBPanel<JBPanel<*>>()
//    panel.layout = GridLayout(2,1)
//    val iconLabel = JLabel(icon)
//    panel.add(iconLabel)
//    val label = JLabel(text)
//    panel.add(label)
//    panel.border = empty()
//    return panel
//}
//
//fun fixedSize(swingComponent: JComponent, dim: Dimension) {
//    swingComponent.minimumSize = dim
//    swingComponent.maximumSize = dim
//}



















