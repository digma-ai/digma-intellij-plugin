package org.digma.intellij.plugin.api

import com.intellij.openapi.project.Project
import io.netty.handler.codec.http.QueryStringDecoder
import org.digma.intellij.plugin.events.OpenDashboardRequest
import org.digma.intellij.plugin.log.Log

class OpenReportCommand : AbstractApiCommand(){

    override suspend fun execute(project: Project, urlDecoder: QueryStringDecoder) {

        Log.log(
            logger::trace,
            "OpenReport called with projectName={}, thread={}",
            project.name,
            Thread.currentThread().name
        )

        project.messageBus.syncPublisher(OpenDashboardRequest.OPEN_DASHBOARD_REQUEST_TOPIC).openReportRequest("Report")

    }
}