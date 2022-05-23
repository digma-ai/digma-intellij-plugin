package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI.Borders.empty
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.JPanel

fun insightItemPanel(panel: JPanel): JPanel {
    val wrapper = JPanel()
    wrapper.border = empty(5)
    wrapper.layout = BorderLayout()
    wrapper.add(panel,BorderLayout.CENTER)
    return wrapper
}

fun insightGroupPanel(panel: JPanel): JPanel {

    panel.isOpaque = true
    panel.background = insightListBackground()
    panel.border = empty()
    val wrapper = JPanel()
    wrapper.layout = BorderLayout()
    wrapper.isOpaque = true
    wrapper.background = insightListBackground()
    wrapper.add(panel,BorderLayout.CENTER)
    wrapper.border = empty()
    return wrapper
}


fun insightListBackground():Color{
    var background: Color = JBColor.namedColor("Editor.background",Color.BLACK)
    if (UIUtil.isUnderDarcula()){
        background = Color(38,38,38)
    }
    return background
}