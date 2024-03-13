package org.digma.intellij.plugin.ui.notifications

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.ui.jcef.JBCefApp
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.jcef.JCefComponent.JCefComponentBuilder
import org.digma.intellij.plugin.ui.list.listBackground
import org.digma.intellij.plugin.ui.panels.DisposablePanel
import java.awt.BorderLayout
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JLabel


class TopNotificationsPanel(private val project: Project) : DisposablePanel() {

    private var jCefComponent: JCefComponent?
    private lateinit var jbPopup: JBPopup

    init {

        jCefComponent = createJcefComponent()

        val jcefUiComponent: JComponent = jCefComponent?.getComponent() ?: JLabel("JCEF not supported")

        layout = BorderLayout()
        border = JBUI.Borders.empty()
        background = listBackground()
        add(jcefUiComponent, BorderLayout.CENTER)
    }

    private fun createJcefComponent(): JCefComponent? {
        return if (JBCefApp.isSupported()) {
            JCefComponentBuilder(project, project.service<NotificationsService>())
                .url(NOTIFICATIONS_URL)
                .addMessageRouterHandler(TopNotificationsMessageRouterHandler(project, this))
                .schemeHandlerFactory(NotificationsSchemeHandlerFactory(project, NotificationViewMode.popup))
                .build()

        } else {
            null
        }
    }

    override fun getInsets(): Insets {
        return JBUI.emptyInsets()
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
        jCefComponent?.dispose()
    }


}
