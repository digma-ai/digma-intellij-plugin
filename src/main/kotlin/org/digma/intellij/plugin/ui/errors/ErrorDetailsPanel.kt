package org.digma.intellij.plugin.ui.errors

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import javax.swing.JPanel


fun errorDetailsPanel(project: Project): JPanel {

    return panel{
        row {
            label("error details panel")
        }
    }

}
