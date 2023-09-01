package org.digma.intellij.plugin.ui.notifications

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.analytics.NoSelectedEnvironmentException
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import org.digma.intellij.plugin.ui.jcef.BaseMessageRouterHandler
import org.digma.intellij.plugin.ui.jcef.executeWindowPostMessageJavaScript
import org.digma.intellij.plugin.ui.jcef.tryGetFieldFromPayload
import org.digma.intellij.plugin.ui.notifications.model.SetNotificationsMessage


abstract class NotificationsMessageRouterHandler(project: Project) : BaseMessageRouterHandler(project) {

    abstract fun doClose()

    abstract fun getNotifications(project: Project, pageNumber: Int, pageSize: Int, isRead: Boolean): String


    /**
     * do the query action. this method is executed on a pooled thread
     */
    override fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, action: String) {

        //exceptions are handles in BaseMessageRouterHandler.onQuery

        when (action) {

            "NOTIFICATIONS/GET_DATA" -> {
                Log.log(logger::trace, project, "got NOTIFICATIONS/GET_DATA message")

                Backgroundable.executeOnPooledThread {

                    Log.log(logger::trace, project, "updating notifications data in background")

                    try {

                        val pageNumber: String = objectMapper.readTree(requestJsonNode.get("payload").toString()).get("pageNumber").asText()
                        val pageSize: String = objectMapper.readTree(requestJsonNode.get("payload").toString()).get("pageSize").asText()
                        val isRead: String = objectMapper.readTree(requestJsonNode.get("payload").toString()).get("isRead").asText()
                        val notificationsJson = getNotifications(project, pageNumber.toInt(), pageSize.toInt(), isRead.toBoolean())
                        Log.log(logger::trace, project, "got notifications {}", notificationsJson)
                        val payload = objectMapper.readTree(notificationsJson)
                        val message = SetNotificationsMessage("digma", "NOTIFICATIONS/SET_DATA", payload)
                        Log.log(logger::trace, project, "sending NOTIFICATIONS/SET_DATA message")
                        executeWindowPostMessageJavaScript(browser, objectMapper.writeValueAsString(message))

                    } catch (e: NoSelectedEnvironmentException) {
                        Log.warnWithException(logger, e, "error setting notifications data, no selected environment")
                    } catch (e: Exception) {
                        Log.warnWithException(logger, e, "error setting notifications data")
                        ErrorReporter.getInstance().reportError(project, "NotificationsMessageRouterHandler.SET_DATA", e)
                    }
                }
            }

            "NOTIFICATIONS/GO_TO_SPAN" -> {
                ActivityMonitor.getInstance(project).registerNotificationCenterEvent("${javaClass.simpleName}SpanClicked", mapOf())
                Log.log(logger::trace, project, "got NOTIFICATIONS/GO_TO_SPAN message")
                doClose()
                val spanCodeObjectId: String = objectMapper.readTree(requestJsonNode.get("payload").toString()).get("spanCodeObjectId").asText()
                project.service<NotificationsService>().goToSpan(spanCodeObjectId)
            }

            "NOTIFICATIONS/CLOSE" -> {
                ActivityMonitor.getInstance(project).registerNotificationCenterEvent("${javaClass.simpleName}Closed", mapOf())
                Log.log(logger::trace, project, "got NOTIFICATIONS/CLOSE message")
                //if there are no notifications upToDateTime will not exist in the payload
                val upToDateTime = tryGetFieldFromPayload(objectMapper, requestJsonNode, "upToDateTime")
                if (upToDateTime == null) {
                    Log.log(logger::warn, project, "could not get upToDateTime in NOTIFICATIONS/CLOSE , not marking notifications as read")
                }
                upToDateTime?.let {
                    Log.log(logger::trace, project, "marking notifications read with {}", it)
                    project.service<NotificationsService>().setReadNotificationsTime(it)
                }
                doClose()
            }
        }

    }
}


class TopNotificationsMessageRouterHandler(project: Project, private val topNotificationsPanel: TopNotificationsPanel) :
    NotificationsMessageRouterHandler(project) {


    override fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, action: String) {


        when (action) {

            "NOTIFICATIONS/GO_TO_NOTIFICATIONS" -> {
                ActivityMonitor.getInstance(project).registerNotificationCenterEvent("NotificationsViewAllClicked", mapOf())
                Log.log(logger::trace, project, "got NOTIFICATIONS/GO_TO_NOTIFICATIONS message")
                doClose()
                EDT.ensureEDT {
                    MainToolWindowCardsController.getInstance(project).showAllNotifications()
                }

            }

            else -> {
                super.doOnQuery(project, browser, requestJsonNode, action)
            }

        }
    }

    override fun doClose() {

        EDT.ensureEDT {
            topNotificationsPanel.close()
        }
    }

    override fun getNotifications(project: Project, pageNumber: Int, pageSize: Int, isRead: Boolean): String {
        //top notifications ignores the query parameters and always loads top 3
        return project.service<NotificationsService>().getNotifications(1, 3, false)
    }
}


class AllNotificationsMessageRouterHandler(project: Project) :
    NotificationsMessageRouterHandler(project) {

    override fun doClose() {
        EDT.ensureEDT {
            MainToolWindowCardsController.getInstance(project).closeAllNotifications()
        }
    }

    override fun getNotifications(project: Project, pageNumber: Int, pageSize: Int, isRead: Boolean): String {

        return project.service<NotificationsService>().getNotifications(pageNumber, pageSize, isRead)
    }
}


