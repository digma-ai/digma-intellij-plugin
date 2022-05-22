package org.digma.intellij.plugin.ui.list.insights

import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel

fun insightItemPanel(panel: JPanel): JPanel {

    panel.border = BorderFactory.createRaisedBevelBorder()

    val wrapper = JPanel()
    wrapper.border = BorderFactory.createRaisedBevelBorder()
    wrapper.layout = BoxLayout(wrapper, BoxLayout.X_AXIS)
    wrapper.add(panel)
    wrapper.add(Box.createVerticalGlue())
    return wrapper
}

fun insightGroupPanel(panel: JPanel): JPanel {
    val wrapper = JPanel()
    wrapper.border = BorderFactory.createEmptyBorder()
    wrapper.layout = BoxLayout(wrapper, BoxLayout.X_AXIS)
    wrapper.add(panel)
    return wrapper
}