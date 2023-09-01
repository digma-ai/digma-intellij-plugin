package org.digma.intellij.plugin.ui.notifications

import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefApp
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.jcef.JCefComponentBuilder
import org.digma.intellij.plugin.ui.list.listBackground
import org.digma.intellij.plugin.ui.panels.DisposablePanel
import java.awt.BorderLayout
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JLabel


class AllNotificationsPanel(private val project: Project) : DisposablePanel() {

    private lateinit var jCefComponent: JCefComponent

    init {
        layout = BorderLayout()
        border = JBUI.Borders.empty()
        background = listBackground()
        add(createComponent(), BorderLayout.CENTER)
    }

    override fun getInsets(): Insets {
        return JBUI.emptyInsets()
    }

    private fun createComponent(): JComponent {
        return if (JBCefApp.isSupported()) {

            jCefComponent = JCefComponentBuilder(project)
                .url(NOTIFICATIONS_URL)
                .messageRouterHandler(AllNotificationsMessageRouterHandler(project, this))
                .schemeHandlerFactory(NotificationsSchemeHandlerFactory(project, NotificationViewMode.full))
                .build()

            jCefComponent.getComponent()

        } else {
            JLabel("JCEF not supported")
        }
    }


    //call when clicking the X button
    fun close() {
        dispose()
    }


    override fun dispose() {
        jCefComponent.dispose()
    }

}
