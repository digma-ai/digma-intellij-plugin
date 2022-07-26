package org.digma.intellij.plugin.ui.common

import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError
import org.digma.intellij.plugin.model.rest.errors.ScoreInfo
import java.awt.*
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.border.LineBorder


fun createScorePanelNoArrows(model: CodeObjectError): JPanel {

    val scorePanelSize = Laf.scalePanels(Laf.Sizes.ERROR_SCORE_PANEL_SIZE)
    val scoreLabel = JLabel("${model.scoreInfo.score}", JLabel.CENTER)
    scoreLabel.toolTipText = genToolTipAsHtml(model.scoreInfo)
    scoreLabel.preferredSize = Dimension(scorePanelSize,scorePanelSize)
    scoreLabel.maximumSize = Dimension(scorePanelSize,scorePanelSize)
    scoreLabel.border = empty()
    scoreLabel.isOpaque = false

    val scorePanel = JPanel(BorderLayout())
    scorePanel.add(scoreLabel,BorderLayout.CENTER)
    scorePanel.isOpaque = false
    scorePanel.border = object: LineBorder(colorOf(model.scoreInfo.score), Laf.scaleBorders(3), true){
        override fun paintBorder(c: Component?, g: Graphics?, x: Int, y: Int, width: Int, height: Int) {
            val g2d = g as Graphics2D
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val oldColor = g2d.color
            g2d.color = lineColor
            val archWidth = 15
            g2d.drawRoundRect(x,y,width-1,height-1,archWidth,archWidth)
            g2d.drawRoundRect(x+1,y+1,width-3,height-3,archWidth-3,archWidth-3)
            g2d.drawRoundRect(x+2,y+2,width-5,height-5,archWidth-7,archWidth-7)
            g2d.color = oldColor
        }
    }

    val result = JPanel()
    result.layout = BorderLayout()
    result.add(scorePanel,BorderLayout.CENTER)
    result.border = empty()
    result.isOpaque = false
    return result
}


//fun createScorePanelWithArrows(model: CodeObjectError): JPanel {
//
//    val scorePanelSize = Laf.scalePanels(Icons.ERROR_SCORE_PANEL_SIZE)
//    val scorePanel = JPanel(BorderLayout())
//    val scoreLabel = JLabel("${model.scoreInfo.score}", JLabel.CENTER)
//    scoreLabel.toolTipText = genToolTipAsHtml(model.scoreInfo)
//    scoreLabel.preferredSize = Dimension(scorePanelSize,scorePanelSize)
//    scoreLabel.maximumSize = Dimension(scorePanelSize,scorePanelSize)
//    scoreLabel.border = empty()
//    scorePanel.add(scoreLabel,BorderLayout.CENTER)
//    scorePanel.border = createLineBorder(colorOf(model.scoreInfo.score), 2, true)
//
//    val iconLabel: JLabel
//    if (model.startsHere) {
//        iconLabel = JLabel(Icons.Error.RAISED_HERE)
//        iconLabel.toolTipText = "Raised here"
//    } else {
//        iconLabel = JLabel(Icons.Error.HANDLED_HERE)
//        iconLabel.toolTipText = "Handled here"
//    }
//
//    val result = JPanel()
//    result.layout = BorderLayout()
//    result.add(scorePanel,BorderLayout.CENTER)
//    result.add(iconLabel, BorderLayout.SOUTH)
//
//    return result
//}






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
            return Laf.Colors.ERROR_GREEN
        }
        if (score <= 80) {
            return Laf.Colors.ERROR_ORANGE
        }
        if (score <= 100) {
            return Laf.Colors.ERROR_RED
        }
    }
    return Color.WHITE
}
