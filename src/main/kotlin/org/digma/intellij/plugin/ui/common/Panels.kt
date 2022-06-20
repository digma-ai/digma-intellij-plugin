package org.digma.intellij.plugin.ui.common

import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI.Borders.empty
import java.awt.*
import javax.swing.*


fun panelOfUnsupported(caption: String): JPanel {
    return panel {
        row("Unsupported yet: '$caption'") {
        }.layout(RowLayout.PARENT_GRID)
    }
}





fun iconPanel(icon: Icon, text: String): JPanel {
    val panel = JPanel()
    panel.layout = BorderLayout(5, 10)
    panel.add(JLabel(icon), BorderLayout.CENTER)
    val label = JLabel(text)
    label.horizontalAlignment = SwingConstants.CENTER
    panel.add(label, BorderLayout.SOUTH)
    panel.border = empty()
    panel.background = Color.BLUE
    return panel
}

fun iconPanelBox(icon: Icon, text: String): JPanel {
    val panel = JPanel()
    panel.layout = BoxLayout(panel,BoxLayout.Y_AXIS)
    val iconLabel = JLabel(icon)
    panel.add(iconLabel)
    val label = JLabel(text)
    panel.add(label)
    panel.border = empty()
    return panel
}

fun iconPanelGrid(icon: Icon, text: String): JPanel {
    val panel = JBPanel<JBPanel<*>>()
    panel.layout = GridLayout(2,1)
    val iconLabel = JLabel(icon)
    val f: Font = iconLabel.font
    iconLabel.font = f.deriveFont(f.style or Font.BOLD)
    panel.add(iconLabel)
    val label = JLabel(text)
    panel.add(label)
    panel.border = empty()
    return panel
}

fun fixedSize(swingComponent: JComponent, dim: Dimension) {
    swingComponent.minimumSize = dim
    swingComponent.maximumSize = dim
}


