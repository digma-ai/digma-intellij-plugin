package org.digma.intellij.plugin.ui.notifications

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.ui.jcef.BaseResourceHandler
import java.io.InputStream

class NotificationsResourceHandler(val project: Project, private val notificationViewMode: NotificationViewMode, path: String) :
    BaseResourceHandler(path) {


    override fun isIndexHtml(path: String): Boolean {
        return path.endsWith("index.html", true)
    }

    override fun buildIndexFromTemplate(path: String): InputStream? {
        return NotificationsIndexTemplateBuilder(notificationViewMode).build(project)
    }
}