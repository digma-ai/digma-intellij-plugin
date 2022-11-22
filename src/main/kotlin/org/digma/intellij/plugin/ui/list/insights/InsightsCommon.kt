package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.model.rest.insights.UnmappedInsight
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.buildBoldTitleGrayedComment
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import org.digma.intellij.plugin.ui.list.commonListItemPanel
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*
import kotlin.math.max


fun insightTitlePanel(panel: JPanel): JPanel {
    panel.isOpaque = false
    panel.border = empty(10, 5,0,5)
    return panel
}

fun insightItemPanel(panel: JPanel): JPanel {
    return commonListItemPanel(panel)
}

fun createInsightPanel(title: String, description: String, icon: Icon, body: JComponent?, buttons: List<JButton?>?, panelsLayoutHelper: PanelsLayoutHelper): JPanel {
    return createInsightPanel(title, description, icon, body, buttons, null, panelsLayoutHelper)
}

fun createInsightPanel(title: String, description: String, icon: Icon, body: JComponent?, footer: JComponent?): JPanel {
    return createInsightPanel(title, description, icon, body, null, footer, null)
}

fun createInsightPanel(title: String, description: String, icon: Icon, body: JComponent?, buttons: List<JButton?>?, footer: JComponent?, panelsLayoutHelper: PanelsLayoutHelper?): JPanel {

    // .-----------------------------------.
    // | title                     | icon  |
    // | description               |       |
    // |-----------------------------------|
    // | body                              |
    // |-----------------------------------|
    // |                           buttons |
    // '-----------------------------------'

    val iconLabel = JLabel(icon, SwingConstants.RIGHT)
    iconLabel.horizontalAlignment = SwingConstants.RIGHT
    iconLabel.verticalAlignment = SwingConstants.TOP
    iconLabel.isOpaque = false
    iconLabel.border = empty(2)

    val messageLabel = JLabel(buildBoldTitleGrayedComment(title,description), SwingConstants.LEFT)
    messageLabel.isOpaque = false
    messageLabel.verticalAlignment = SwingConstants.TOP

    val result = JBPanel<JBPanel<*>>()

    result.layout = BorderLayout()
    result.add(messageLabel,BorderLayout.CENTER)
    result.add(iconLabel,BorderLayout.EAST)

    if(body != null || buttons != null){
        val bodyWrapper = JPanel(BorderLayout())
        bodyWrapper.isOpaque = false

        if(body != null)
            bodyWrapper.add(body, BorderLayout.CENTER)

        if(buttons != null){
            val buttonsList = JPanel(FlowLayout(FlowLayout.RIGHT, 0 ,0))
            buttonsList.isOpaque = false
            buttonsList.border = empty()
            buttons.filterNotNull().forEach {
                buttonsList.add(Box.createHorizontalStrut(5))
                buttonsList.add(it)
            }
            bodyWrapper.add(buttonsList, BorderLayout.SOUTH)
        }

        if(footer != null){
            val footerComponent = JPanel(FlowLayout(FlowLayout.LEFT, 0 ,0))
            footerComponent.isOpaque = false
            footerComponent.border = empty()

            footerComponent.add(Box.createHorizontalStrut(5))
            footerComponent.add(footer)

            bodyWrapper.add(footerComponent, BorderLayout.SOUTH)
        }

        result.add(bodyWrapper,BorderLayout.SOUTH)
    }


    return insightItemPanel(result)
}


fun unmappedInsightPanel(modelObject: UnmappedInsight, panelsLayoutHelper: PanelsLayoutHelper): JPanel {

    val methodName = modelObject.codeObjectId.substringAfterLast("\$_\$")
    return createInsightPanel("Unmapped insight: '${modelObject.theType}'",
        "unmapped insight type for '$methodName'",
        Laf.Icons.Insight.QUESTION_MARK, null, null, panelsLayoutHelper)
}


fun genericPanelForSingleInsight(modelObject: Any?, panelsLayoutHelper: PanelsLayoutHelper): JPanel {

    return createInsightPanel("Generic insight panel",
        "Insight named ${modelObject?.javaClass?.simpleName}",
        Laf.Icons.Insight.QUESTION_MARK,null, null, panelsLayoutHelper)
}





internal fun insightsIconPanelBorder(icon: Icon, text: String, panelsLayoutHelper: PanelsLayoutHelper): JPanel {

    val panel = InsightAlignedPanel(panelsLayoutHelper)
    panel.layout = BorderLayout()
    panel.isOpaque = false
    panel.border = empty(2,0,0, getInsightIconPanelRightBorderSize())

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
        border = empty(0,0,0, getInsightIconPanelRightBorderSize())
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