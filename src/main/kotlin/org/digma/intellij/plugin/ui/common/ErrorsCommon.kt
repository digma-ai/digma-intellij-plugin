package org.digma.intellij.plugin.ui.common

import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError
import org.digma.intellij.plugin.model.rest.errors.ScoreInfo
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel


fun createScorePanel(model: CodeObjectError): JPanel {
    val lineBorder = BorderFactory.createLineBorder(colorOf(model.scoreInfo.score), 2, true)
    val scoreToolTip = genToolTipAsHtml(model.scoreInfo)

    val scorePanel = JPanel(FlowLayout())
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

    val result = JPanel()

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
            return Swing.ERROR_GREEN
        }
        if (score <= 80) {
            return Swing.ERROR_ORANGE
        }
        if (score <= 100) {
            return Swing.ERROR_RED
        }
    }
    return Color.WHITE
}
