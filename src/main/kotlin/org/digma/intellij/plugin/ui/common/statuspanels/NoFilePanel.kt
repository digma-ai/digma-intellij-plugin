package org.digma.intellij.plugin.ui.common.statuspanels

import com.intellij.openapi.project.Project
import javax.swing.JPanel


fun createNoFilePanel(project: Project):JPanel{
    return createNoFileInEditorPanel(project,"No File Opened")
}
