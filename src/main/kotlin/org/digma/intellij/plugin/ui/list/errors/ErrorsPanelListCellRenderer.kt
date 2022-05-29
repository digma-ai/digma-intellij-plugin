package org.digma.intellij.plugin.ui.list.errors

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError
import org.digma.intellij.plugin.model.rest.errors.ScoreInfo
import org.digma.intellij.plugin.ui.common.Swing.ERROR_GREEN
import org.digma.intellij.plugin.ui.common.Swing.ERROR_ORANGE
import org.digma.intellij.plugin.ui.common.Swing.ERROR_RED
import org.digma.intellij.plugin.ui.list.AbstractPanelListCellRenderer
import org.digma.intellij.plugin.ui.list.insights.insightItemPanel
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.Color
import javax.swing.JPanel
import javax.swing.border.LineBorder

class ErrorsPanelListCellRenderer : AbstractPanelListCellRenderer() {


    override fun createPanel(project: Project, value: ListViewItem<*>, index: Int): JPanel {
        return getOrCreatePanel(project, index, value as ListViewItem<CodeObjectError>)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getOrCreatePanel(project: Project, index: Int, value: ListViewItem<CodeObjectError>): JPanel {

        val model = value.modelObject

        val result = panel {
            //temporary: need to implement logic
            row {
                link(model.name) {
                    //error.codeObjectId
                    println("In action")
                }
                var from = "From me"
//                if (insight.codeObjectId != error.sourceCodeObjectId) {
//                    from = "From ${error.sourceCodeObjectId.split("\$_\$")[0]}"
//                }
                label(from)
                label("Score: ${model.scoreInfo.score}")
            }
        }

        return result
    }

}

private fun createSingleErrorPanel(model: CodeObjectError): JPanel {
    val title = panel {
        row {
            link(model.name) { actionEvent ->
                {
                    //TODO: implement the link
                }
            }.verticalAlign(VerticalAlign.TOP)
        }
    }
    title.border = JBUI.Borders.empty(0)

//    val iconPanel = iconPanelGrid(icon, iconText)
//    iconPanel.border = JBUI.Borders.empty(0)
//    iconPanel.co
//
//    val contentPanel = JBPanel<JBPanel<*>>()
//    contentPanel.layout = BorderLayout(0, 0)
//
//    val message = JLabel(body, SwingConstants.LEFT)
//
//    contentPanel.add(title, BorderLayout.NORTH)
//    contentPanel.add(message, BorderLayout.CENTER)
//
    val result = JBPanel<JBPanel<*>>()
//    result.layout = BoxLayout(result, BoxLayout.X_AXIS)
//    result.add(contentPanel)
//    result.add(Box.createHorizontalStrut(5))
//    result.add(iconPanel)
//
    return insightItemPanel(result)
}

private fun createScorePanel(model: CodeObjectError): JBPanel<JBPanel<*>> {
    val lineBorder = LineBorder(colorOf(model.scoreInfo.score), 2, true);
    val toolTip = genToolTipAsHtml(model.scoreInfo)

    return JBPanel<JBPanel<*>>();
}

private fun genToolTipAsHtml(scoreInfo: ScoreInfo): String {
    val sb = StringBuilder()
    var firstTime = true
    scoreInfo.scoreParams
        .forEach { (key, value) ->
            if (value > 0) {
                if (firstTime) {
                    firstTime = false
                } else {
                    sb.append("<br>")
                }
                sb.append("$key: $value")
            }
        }
    return sb.toString()
}

private fun colorOf(score: Int?): Color {
    if (score != null) {
        if (score <= 40) {
            return ERROR_GREEN
        }
        if (score <= 80) {
            return ERROR_ORANGE
        }
        if (score <= 100) {
            return ERROR_RED
        }
    }
    return Color.BLACK
}
