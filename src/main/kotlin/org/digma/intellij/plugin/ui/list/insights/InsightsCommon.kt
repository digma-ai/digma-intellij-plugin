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
import org.digma.intellij.plugin.ui.list.listGroupPanel
import org.digma.intellij.plugin.ui.list.listItemPanel
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import kotlin.math.max

fun insightItemPanel(panel: JPanel): JPanel {
    return listItemPanel(panel)
}

fun insightGroupPanel(panel: JPanel): JPanel {
    return listGroupPanel(panel)
}


fun createInsightPanel(title: String, body: String, icon: Icon, iconText: String): JPanel {
    return createInsightPanel(title, body, icon, iconText, true)
}

fun createInsightPanel(title: String, body: String, icon: Icon, iconText: String, wrap: Boolean): JPanel {

    val message = JLabel(buildBoldTitleGrayedComment(title,body),SwingConstants.LEFT)
    val iconPanel = insightsIconPanelBorder(icon, iconText)
    val result = JBPanel<JBPanel<*>>()
    result.layout = BorderLayout()
    result.add(message,BorderLayout.CENTER)
    result.add(iconPanel,BorderLayout.EAST)
    result.border = empty()

    return if (wrap) {
        insightItemPanel(result)
    } else {
        result
    }
}


fun unmappedInsightPanel(listViewItem: ListViewItem<UnmappedInsight>): JPanel {

    val methodName = listViewItem.modelObject.codeObjectId.substringAfterLast("\$_\$")
    return createInsightPanel("Unmapped insight: '${listViewItem.modelObject.theType}'",
        "unmapped insight type for '$methodName'",
        Icons.QUESTION_MARK, "")
}


fun genericPanelForSingleInsight(listViewItem: ListViewItem<CodeObjectInsight>): JPanel {

    return createInsightPanel("Generic insight panel",
        "Insight named ${listViewItem.modelObject.javaClass.simpleName}",
        Icons.QUESTION_MARK, "")
}








internal fun insightsIconPanelBorder(icon: Icon, text: String): JPanel {
    val iconLabel = JLabel(icon)
    val textLabel = JLabel(text)

    val panel: JPanel = object: JPanel(){
        override fun getPreferredSize(): Dimension {
            val ps = super.getPreferredSize()
            if (ps == null){
                return ps
            }
//            if (isVisible) {
                val h = ps.height
                val w = ps.width
                addCurrentLargestWidthIconPanel(w)
                return Dimension(getCurrentLargestWidthIconPanel(w), h)
//            }else{
//                return ps
//            }
        }
    }

    panel.layout = BorderLayout()
    iconLabel.horizontalAlignment = SwingConstants.CENTER
    panel.add(iconLabel, BorderLayout.CENTER)
    textLabel.horizontalAlignment = SwingConstants.CENTER
    panel.add(textLabel, BorderLayout.SOUTH)
    panel.border = empty(0,0,0,Laf.scaleBorders(getInsightIconPanelRightBorderSize()))

    val width = panel.preferredSize.width
    addCurrentLargestWidthIconPanel(width)

    return panel
}


internal fun getInsightIconPanelRightBorderSize():Int{
    return 20
}
internal fun getCurrentLargestWidthIconPanel(width: Int):Int{
    //this method should never return null and never throw NPE
    val currentLargest: Int =
        (InsightsPanelsLayoutHelper.getObjectAttribute("insightsIconPanelBorder","largestWidth")?: 0) as Int
    return max(width,currentLargest)
}
internal fun addCurrentLargestWidthIconPanel(width: Int){
    //this method should never throw NPE
    val currentLargest: Int =
        (InsightsPanelsLayoutHelper.getObjectAttribute("insightsIconPanelBorder","largestWidth")?: 0) as Int
    InsightsPanelsLayoutHelper.addObjectAttribute("insightsIconPanelBorder","largestWidth",
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



















