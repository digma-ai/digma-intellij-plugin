package org.digma.intellij.plugin.toolwindow

import com.intellij.openapi.project.Project

class CustomViewerWindowService(val project: Project) {
    val customViewerWindow = CustomViewerWindow(project)
}