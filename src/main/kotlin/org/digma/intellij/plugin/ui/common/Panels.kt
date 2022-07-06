package org.digma.intellij.plugin.ui.common

import com.intellij.icons.AllIcons
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.ui.JBUI
import javax.swing.JPanel


fun noCodeObjectWarningPanel(text: String): JPanel {

    val panel =  panel {
        row{
            icon(AllIcons.General.BalloonInformation)
                .horizontalAlign(HorizontalAlign.CENTER)
        }.bottomGap(BottomGap.MEDIUM).topGap(TopGap.MEDIUM)
        row{
            label(text)
                .horizontalAlign(HorizontalAlign.CENTER)
        }.bottomGap(BottomGap.MEDIUM).topGap(TopGap.MEDIUM)
    }

    panel.border = JBUI.Borders.empty()
    panel.isOpaque = false
    return panel
}

