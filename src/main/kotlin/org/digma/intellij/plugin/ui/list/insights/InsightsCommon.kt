package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.model.rest.insights.UnmappedInsight
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.htmlSpanSmoked
import org.digma.intellij.plugin.ui.common.htmlSpanTitle
import org.digma.intellij.plugin.ui.common.iconPanelBorder
import org.digma.intellij.plugin.ui.list.listGroupPanel
import org.digma.intellij.plugin.ui.list.listItemPanel
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.BorderLayout
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

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
    val message = JLabel(asHtml("${htmlSpanTitle()}<b>$title</b><br>${htmlSpanSmoked()}$body"), SwingConstants.LEFT)

    val iconPanel = iconPanelBorder(icon, iconText)

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