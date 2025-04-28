package org.digma.intellij.plugin.ui.jcef.pluginapi

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.common.objectToJson
import org.digma.intellij.plugin.ui.jcef.jsonToObject
import org.digma.intellij.plugin.ui.recentactivity.RecentActivityToolWindowIconChanger
import java.beans.ConstructorProperties

class RecentActivityBadgeCommand : Command() {

    override fun execute(
        project: Project,
        requestId: Long,
        requestMethod: String,
        postData: ByteArray?,
        headers: Map<String, String>
    ): PluginApiHttpResponse {

        try {
            val badgeRequest = postData?.let { jsonToObject(it, BadgeRequest::class.java) }

            if (badgeRequest == null) {
                // Return 400 Bad Request, with a small error body
                val error = ErrorData("Invalid or missing badge request")
                val errorJson = objectToJson(error).toByteArray()
                return PluginApiHttpResponse(
                    status = 400,
                    headers = mutableMapOf(
                        "Content-Type" to "application/json",
                        "Content-Length" to errorJson.size.toString()
                    ),
                    contentLength = errorJson.size.toLong(),
                    contentType = "application/json",
                    contentStream = errorJson.inputStream()
                )
            }

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
            val errorMessage = e.message ?: e.toString()
            val error = ErrorData(errorMessage)
            val errorJson = objectToJson(error).toByteArray()
            return PluginApiHttpResponse(
                status = 500,
                headers = mutableMapOf(
                    "Content-Type" to "application/json",
                    "Content-Length" to errorJson.size.toString()
                ),
                contentLength = errorJson.size.toLong(),
                contentType = "application/json",
                contentStream = errorJson.inputStream()
            )
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
