package org.digma.intellij.plugin.ui.common

import com.intellij.icons.AllIcons
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.ui.JBUI
import java.awt.Color
import javax.swing.JPanel


fun noCodeObjectWarningPanel(text: String): JPanel {

    val result =  panel {
        row{
            icon(AllIcons.General.NotificationWarning)
                .horizontalAlign(HorizontalAlign.CENTER)
        }.bottomGap(BottomGap.MEDIUM)
        row{
            label(text)
                .horizontalAlign(HorizontalAlign.CENTER)
        }.bottomGap(BottomGap.MEDIUM)
    }

    result.border = JBUI.Borders.customLine(Color.BLACK,1)
    result.background = Swing.NO_INFO_PANEL_BACKGROUND
    return result
}

