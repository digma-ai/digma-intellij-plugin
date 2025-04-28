package org.digma.intellij.plugin.ui.jcef.pluginapi

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import org.digma.intellij.plugin.PluginId
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.objectToJson
import org.digma.intellij.plugin.icons.AppIcons
import org.digma.intellij.plugin.ui.jcef.jsonToObject
import java.beans.ConstructorProperties
import javax.swing.Icon

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

            val recentActivityToolWindowIconChanger = RecentActivityToolWindowIconChanger(project)
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


private class RecentActivityToolWindowIconChanger(val project: Project) {

    private val defaultIcon = AppIcons.TOOL_WINDOW_OBSERVABILITY
    private var actualIcon: Icon? = null
    private var badgeIcon: Icon? = null


    fun showBadge() {

        val toolWindow = getToolWindow() ?: return

        createBadgeIcon()

        EDT.ensureEDT {
            badgeIcon?.let {
                toolWindow.setIcon(it)
            }
        }
    }

    private fun createBadgeIcon() {

        if (badgeIcon == null) {
            val icon = actualIcon ?: defaultIcon
            badgeIcon = ExecutionUtil.getLiveIndicator(icon)
        }
    }


    fun hideBadge() {

        //if badgeIcon is null then it was never created and no need to do anything
        if (badgeIcon == null) {
            return
        }

        val toolWindow = getToolWindow() ?: return

        EDT.ensureEDT {
            toolWindow.setIcon(actualIcon ?: defaultIcon)
        }
    }

    private fun getToolWindow(): ToolWindow? {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(PluginId.OBSERVABILITY_WINDOW_ID)

        if (actualIcon == null) {
            //capture the actual icon the first time we got a non-null tool window.
            // and make sure it is initialized at least to the default icon
            actualIcon = toolWindow?.icon ?: defaultIcon
        }

        return toolWindow
    }

}