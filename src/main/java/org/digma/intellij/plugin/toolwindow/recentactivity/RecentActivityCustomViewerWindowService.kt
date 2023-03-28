package org.digma.intellij.plugin.toolwindow.recentactivity

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.toolwindow.common.CustomViewerWindow

private const val RECENT_ACTIVITY_RESOURCE_FOLDER_NAME = "recentactivity"
class RecentActivityCustomViewerWindowService(val project: Project) {
    val customViewerWindow = CustomViewerWindow(project, RECENT_ACTIVITY_RESOURCE_FOLDER_NAME)
}