package org.digma.intellij.plugin.ui.list.errors

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.discovery.CodeObjectInfo.Companion.extractMethodName
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError
import org.digma.intellij.plugin.model.rest.errors.ScoreInfo
import org.digma.intellij.plugin.ui.common.Swing.ERROR_GREEN
import org.digma.intellij.plugin.ui.common.Swing.ERROR_ORANGE
import org.digma.intellij.plugin.ui.common.Swing.ERROR_RED
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.fixedSize
import org.digma.intellij.plugin.ui.list.AbstractPanelListCellRenderer
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import org.ocpsoft.prettytime.PrettyTime
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.util.*
import javax.swing.*


class ErrorsPanelListCellRenderer : AbstractPanelListCellRenderer() {


    override fun createPanel(project: Project, value: ListViewItem<*>, index: Int): JPanel {
        return getOrCreatePanel(project, index, value as ListViewItem<CodeObjectError>)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getOrCreatePanel(project: Project, index: Int, value: ListViewItem<CodeObjectError>): JPanel {

        val model = value.modelObject

//        val result = simpleDataPanel(model)
        val result = createSingleErrorPanel(model)

        return result
    }

}

private fun simpleDataPanel(model: CodeObjectError): DialogPanel {
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

private fun createSingleErrorPanel(model: CodeObjectError): JPanel {
    val contents = panel {
        row {
            link(asHtml(model.name)) { actionEvent ->
                {
                    //TODO: implement the link
                }
            }.verticalAlign(VerticalAlign.TOP)

            val relativeFrom: String;
            if (model.startsHere) {
                relativeFrom = "me"
            } else {
                relativeFrom = extractMethodName(model.sourceCodeObjectId)
            }
            label(asHtml(" from $relativeFrom"))
        }
        row {
            label(model.characteristic)
                .bold()
        }
        row {
            label(contentAsHtmlOfFirstAndLast(model))
        }
    }
    contents.border = JBUI.Borders.empty(0)

    val scorePanel = createScorePanel(model)

    val result = JBPanel<JBPanel<*>>()
    result.layout = BoxLayout(result, BoxLayout.X_AXIS)
    result.add(contents)
    result.add(Box.createHorizontalStrut(5))
    result.add(scorePanel)

    return result
}

private fun prettyTimeOf(date: Date): String {
    val ptNow = PrettyTime()
    return ptNow.format(date)
}

private fun contentAsHtmlOfFirstAndLast(model: CodeObjectError): String {
    return asHtml(
        "Started: <b>${prettyTimeOf(model.firstOccurenceTime)}</b>" +
                "  Last: <b>${prettyTimeOf(model.lastOccurenceTime)}<b>"
    )
}

private fun createScorePanel(model: CodeObjectError): JPanel {
    val lineBorder = BorderFactory.createLineBorder(colorOf(model.scoreInfo.score), 2, true);
    val scoreToolTip = genToolTipAsHtml(model.scoreInfo)

    var scorePanel = JPanel(FlowLayout())
    fixedSize(scorePanel, Dimension(48, 48))
    val scoreLabel = JLabel("${model.scoreInfo.score}", JLabel.CENTER)
    scoreLabel.toolTipText = scoreToolTip
    scoreLabel.size = Dimension(32, 32)
    scorePanel.add(scoreLabel)
    scorePanel.border = lineBorder

    val iconLabel: JLabel
    if (model.startsHere) {
        iconLabel = JLabel(Icons.Error.RAISED_HERE)
        iconLabel.toolTipText = "Raised here"
    } else {
        iconLabel = JLabel(Icons.Error.HANDLED_HERE)
        iconLabel.toolTipText = "Handled here"
    }

    var result = JPanel();

    result.layout = BoxLayout(result, BoxLayout.Y_AXIS)
    result.add(scorePanel)
    result.add(iconLabel, BorderLayout.EAST)

    return result
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
    return asHtml(sb.toString())
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
    return Color.WHITE
}
