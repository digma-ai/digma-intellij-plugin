package org.digma.intellij.plugin.ui.list

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.JPanel


fun panelListBackground(): Color {
    var default = Color.DARK_GRAY
    if (UIUtil.isUnderDarcula()) {
        default = Color(38, 38, 38)
    }
    return JBColor.namedColor("Editor.background", default)
}


fun listItemPanel(panel: JPanel): JPanel {
    panel.border = JBUI.Borders.empty(5)
    val wrapper = JPanel()
    wrapper.layout = BorderLayout()
    wrapper.add(panel, BorderLayout.CENTER)
    wrapper.border = JBUI.Borders.empty(5)
    wrapper.isOpaque = false
    return wrapper
}

fun listGroupPanel(panel: JPanel): JPanel {
    panel.isOpaque = false
    panel.border = JBUI.Borders.empty()
    val wrapper = JPanel()
    wrapper.layout = BorderLayout()
    wrapper.add(panel, BorderLayout.CENTER)
    wrapper.isOpaque = false
    wrapper.border = JBUI.Borders.empty()
    return wrapper
}