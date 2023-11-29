package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.assets.AssetsPanel
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JPanel


class HomePanel(project: Project) : JPanel() {


    init {

        isOpaque = false
        layout = BorderLayout(0, 0)
        border = JBUI.Borders.empty()
        add(createAssetsPanel(project), BorderLayout.CENTER)
    }


    private fun createAssetsPanel(project: Project): Component {
        return AssetsPanel(project)
    }

}