package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import java.awt.BorderLayout
import javax.swing.Icon
import javax.swing.JPanel


class ProjectHomePanel(val project: Project) : JPanel() {

    init {
        layout = BorderLayout()
        this.background = Laf.Colors.EDITOR_BACKGROUND
        this.isOpaque = true

        add(createPanel(), BorderLayout.CENTER)
    }


    private fun createPanel(): JPanel {
        return panel {
            row {
                label(asHtml(" "))
                icon(getProjectIcon())
                label(project.name)
            }.layout(RowLayout.LABEL_ALIGNED)
        }.andTransparent()
    }


    private fun getProjectIcon(): Icon {
        return if (JBColor.isBright()) {
            Laf.Icons.General.PROJECT_LIGHT
        } else {
            Laf.Icons.General.PROJECT_DARK
        }
    }
}
