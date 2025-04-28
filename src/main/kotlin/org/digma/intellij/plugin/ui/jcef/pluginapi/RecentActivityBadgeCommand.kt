package org.digma.intellij.plugin.ui.jcef.pluginapi

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.jcef.jsonToObject
import org.digma.intellij.plugin.ui.recentactivity.RecentActivityToolWindowIconChanger
import java.beans.ConstructorProperties

class RecentActivityBadgeCommand : Command() {

    private val logger = Logger.getInstance(this::class.java)

    override fun execute(
        project: Project,
        requestId: Long,
        requestMethod: String,
        postData: ByteArray?,
        headers: Map<String, String>
    ): PluginApiHttpResponse {

        try {
            Log.log(logger::trace, "recent activity badge command received request id {}", requestId)
            val badgeRequest = postData?.let { jsonToObject(it, BadgeRequest::class.java) }
            Log.log(logger::trace, "recent activity badge command received request id {}, badgeRequest {}", requestId, badgeRequest)

            if (badgeRequest == null) {
                Log.log(logger::warn, "recent activity badge command received request id {}, invalid or missing badge request", requestId)
                // Return 400 Bad Request, with a small error body
                val error = ErrorData("Invalid or missing badge request")
                return PluginApiHttpResponse.createErrorResponse(400, error)
            }

            Log.log(logger::trace, "recent activity badge command received request id {}, badgeRequest {}, setting recent activity tool window icon", requestId, badgeRequest)
            val recentActivityToolWindowIconChanger = RecentActivityToolWindowIconChanger.getInstance(project)
            if (badgeRequest.status) {
                recentActivityToolWindowIconChanger.showBadge()
            } else {
                recentActivityToolWindowIconChanger.hideBadge()
            }

            return PluginApiHttpResponse(
                status = 200,
                headers = headers.toMutableMap(),
                contentLength = 0,
                contentType = null,
                contentStream = null
            )
        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "recent activity badge command failed, [request id:{}]", requestId)
            val error = ErrorData(e.message ?: e.toString())
            return PluginApiHttpResponse.createErrorResponse(500, error)
        }
    }
}

data class BadgeRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("status")
constructor(
    @get:JsonProperty("status")
    @param:JsonProperty("status")
    val status: Boolean
)
