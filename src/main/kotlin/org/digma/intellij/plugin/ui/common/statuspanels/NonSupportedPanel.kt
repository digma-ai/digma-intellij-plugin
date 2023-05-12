package org.digma.intellij.plugin.ui.common.statuspanels

import com.intellij.openapi.project.Project
import javax.swing.JPanel


fun createNonSupportedPanel(project: Project):JPanel{
    return createNoFileInEditorPanel(project,"File Type is Not Supported")
}
