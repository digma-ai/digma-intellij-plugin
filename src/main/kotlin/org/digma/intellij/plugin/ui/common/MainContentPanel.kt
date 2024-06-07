package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.mainapp.MainAppPanel
import java.awt.BorderLayout
import java.awt.Insets
import javax.swing.JPanel

class MainContentPanel(project: Project) : JPanel() {

    init {
        isOpaque = false
        border = JBUI.Borders.empty()
        layout = BorderLayout()

        val mainAppPanel = MainAppPanel(project)
        add(mainAppPanel, BorderLayout.CENTER)

    }

    override fun getInsets(): Insets {
        return JBUI.emptyInsets()
    }
}