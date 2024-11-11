package org.digma.intellij.plugin.events

import com.intellij.util.messages.Topic

interface OpenDashboardRequest {

    companion object {
        @JvmStatic
        @Topic.ProjectLevel
        val OPEN_DASHBOARD_REQUEST_TOPIC: Topic<OpenDashboardRequest> = Topic.create(
            "OPEN_DASHBOARD_REQUEST",
            OpenDashboardRequest::class.java
        )
    }

    fun openReportRequest(dashboardName: String)
    fun openDashboardRequest(dashboardName: String)

}
