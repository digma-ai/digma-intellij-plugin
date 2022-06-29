package org.digma.intellij.plugin.ui.common

import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError
import org.digma.intellij.plugin.model.rest.errors.ScoreInfo
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import javax.swing.BorderFactory.createLineBorder
import javax.swing.JLabel
import javax.swing.JPanel


fun createScorePanel(model: CodeObjectError): JPanel {

    val scorePanel = JPanel(BorderLayout())
    val scoreLabel = JLabel("${model.scoreInfo.score}", JLabel.CENTER)
    scoreLabel.toolTipText = genToolTipAsHtml(model.scoreInfo)
    scoreLabel.preferredSize = Dimension(Icons.ERROR_SCORE_PANEL_SIZE,Icons.ERROR_SCORE_PANEL_SIZE)
    scoreLabel.border = empty()
    scorePanel.add(scoreLabel,BorderLayout.CENTER)
    scorePanel.border = createLineBorder(colorOf(model.scoreInfo.score), 2, true)

    val iconLabel: JLabel
    if (model.startsHere) {
        iconLabel = JLabel(Icons.Error.RAISED_HERE)
        iconLabel.toolTipText = "Raised here"
    } else {
        iconLabel = JLabel(Icons.Error.HANDLED_HERE)
        iconLabel.toolTipText = "Handled here"
    }

    val result = JPanel()
    result.layout = BorderLayout()
    result.add(scorePanel,BorderLayout.CENTER)
    result.add(iconLabel, BorderLayout.SOUTH)

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
