package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.UnmappedInsight
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.buildBoldTitleGrayedComment
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import org.digma.intellij.plugin.ui.list.commonListItemPanel
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import kotlin.math.max

fun insightTitlePanel(panel: JPanel): JPanel {
    panel.isOpaque = false
    panel.border = empty(10, 5,0,5)
    return panel
}

fun insightItemPanel(panel: JPanel): JPanel {
    return commonListItemPanel(panel)
}


fun createInsightPanel(title: String, body: String, icon: Icon, iconText: String, panelsLayoutHelper: PanelsLayoutHelper): JPanel {
    return createInsightPanel(title, body, icon, iconText, true,panelsLayoutHelper)
}

@Deprecated("remove,wrap is always true")
private fun createInsightPanel(title: String, body: String, icon: Icon, iconText: String, wrap: Boolean, panelsLayoutHelper: PanelsLayoutHelper): JPanel {

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


fun unmappedInsightPanel(modelObject: UnmappedInsight, panelsLayoutHelper: PanelsLayoutHelper): JPanel {

    val methodName = modelObject.codeObjectId.substringAfterLast("\$_\$")
    return createInsightPanel("Unmapped insight: '${modelObject.theType}'",
        "unmapped insight type for '$methodName'",
        Icons.QUESTION_MARK, "",panelsLayoutHelper)
}


fun genericPanelForSingleInsight(modelObject: Any?, panelsLayoutHelper: PanelsLayoutHelper): JPanel {

    return createInsightPanel("Generic insight panel",
        "Insight named ${modelObject?.javaClass?.simpleName}",
        Icons.QUESTION_MARK, "",panelsLayoutHelper)
}





internal fun insightsIconPanelBorder(icon: Icon, text: String, panelsLayoutHelper: PanelsLayoutHelper): JPanel {

    val panel = InsightAlignedPanel(panelsLayoutHelper)
    panel.layout = BorderLayout()
    panel.isOpaque = false
    panel.border = empty(2,0,0,Laf.scaleBorders(getInsightIconPanelRightBorderSize()))

    val iconLabel = JLabel(icon)
    iconLabel.horizontalAlignment = SwingConstants.CENTER
    panel.add(iconLabel, BorderLayout.CENTER)

    if (text.isNotBlank()) {
        val textLabel = JLabel(text)
        textLabel.horizontalAlignment = SwingConstants.CENTER
        panel.add(textLabel, BorderLayout.SOUTH)
    }

    addCurrentLargestWidthIconPanel(panelsLayoutHelper,panel.preferredSize.width)

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



class InsightAlignedPanel(private val layoutHelper: PanelsLayoutHelper): JPanel(){

    init {
        border = empty(0,0,0,Laf.scaleBorders(getInsightIconPanelRightBorderSize()))
    }
    override fun getPreferredSize(): Dimension {
        val ps = super.getPreferredSize()
        if (ps == null){
            return ps
        }
        val h = ps.height
        val w = ps.width
        addCurrentLargestWidthIconPanel(layoutHelper,w)
        return Dimension(getCurrentLargestWidthIconPanel(layoutHelper,w), h)
    }
}