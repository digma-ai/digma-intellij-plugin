package org.digma.intellij.plugin.analytics

import com.intellij.openapi.project.Project

/**
 * This service keeps track of the analytics service connection status. it is used to decide when to show the
 * no connection message in the plugin window.
 * see also class NoConnectionPanel
 */

interface BackendConnectionMonitor: AnalyticsServiceConnectionEvent {

    companion object {
        @JvmStatic
        fun getInstance(project: Project): BackendConnectionMonitor {
            return project.getService(BackendConnectionMonitor::class.java)
        }
    }
  

    fun isConnectionError(): Boolean 

    fun isConnectionOk(): Boolean
    

}