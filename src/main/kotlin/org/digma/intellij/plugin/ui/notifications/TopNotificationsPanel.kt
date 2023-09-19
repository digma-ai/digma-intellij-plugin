package org.digma.intellij.plugin.ui.notifications

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
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


class TopNotificationsPanel(private val project: Project) : DisposablePanel() {

    private lateinit var jCefComponent: JCefComponent
    private lateinit var jbPopup: JBPopup

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
                .messageRouterHandler(TopNotificationsMessageRouterHandler(project, this))
                .schemeHandlerFactory(NotificationsSchemeHandlerFactory(project, NotificationViewMode.popup))
                .build()

            jCefComponent.getComponent()

        } else {
            JLabel("JCEF not supported")
        }
    }


    //call when clicking the X button or view all
    fun close() {
        dispose()
        jbPopup.closeOk(null)
    }

    fun setPopup(jbPopup: JBPopup) {
        this.jbPopup = jbPopup
    }

    override fun dispose() {
        jCefComponent.dispose()
    }


}
