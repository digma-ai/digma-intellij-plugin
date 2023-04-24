package org.digma.intellij.plugin.toolwindow.recentactivity

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.toolwindow.common.CustomViewerWindow

private const val RECENT_ACTIVITY_RESOURCE_FOLDER_NAME = "recentactivity"
private const val RECENT_ACTIVITY_EXPIRATION_LIMIT = "recentActivityExpirationLimit"

class RecentActivityCustomViewerWindowService(val project: Project) {
    fun getCustomViewerWindow(recentTimeout: Int): CustomViewerWindow{
        return CustomViewerWindow(project, RECENT_ACTIVITY_RESOURCE_FOLDER_NAME,
            mapOf(
                RECENT_ACTIVITY_EXPIRATION_LIMIT to recentTimeout
            ))
    }
}