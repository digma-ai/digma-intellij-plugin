package org.digma.intellij.plugin.ui.common

import com.intellij.icons.AllIcons
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.digma.intellij.plugin.ui.list.ScrollablePanelList
import java.awt.BorderLayout
import javax.swing.JPanel


fun buildPreviewListPanel(list: ScrollablePanelList): JPanel {
    val previewTitle = panel {
        row {
            icon(AllIcons.Ide.FatalErrorRead)
                .horizontalAlign(HorizontalAlign.CENTER)
        }
        row {
            label("No code object was selected")
                .horizontalAlign(HorizontalAlign.CENTER)
        }
        row {
            label("Try to click one of the following code objects")
        }
    }

    previewTitle.isOpaque = false
    val previewPanel = JPanel(BorderLayout())
    previewPanel.add(previewTitle, BorderLayout.NORTH)
    previewPanel.add(list, BorderLayout.CENTER)
    previewPanel.isOpaque = false
    return previewPanel
}

