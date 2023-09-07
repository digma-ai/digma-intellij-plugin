package org.digma.intellij.plugin.ui.recentactivity

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.jcef.JCefComponentBuilder
import org.digma.intellij.plugin.ui.jcef.JaegerButtonStateListener
import org.digma.intellij.plugin.ui.list.listBackground
import org.digma.intellij.plugin.ui.panels.DisposablePanel
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JLabel

class RecentActivityPanel(val project: Project) : DisposablePanel() {


    private lateinit var jCefComponent: JCefComponent

    init {
        layout = BorderLayout()
        border = JBUI.Borders.empty()
        background = listBackground()
        add(createComponent(), BorderLayout.CENTER)

        Disposer.register(project.service<RecentActivityService>()) {
            dispose()
        }

        JaegerButtonStateListener().start(project.service<RecentActivityService>(), jCefComponent)

        project.service<RecentActivityUpdater>().setJcefComponent(jCefComponent)
        project.service<LiveViewUpdater>().setJcefComponent(jCefComponent)
    }

    private fun createComponent(): JComponent {

        return if (JBCefApp.isSupported()) {

            jCefComponent = JCefComponentBuilder(project)
                .url(RECENT_ACTIVITY_URL)
                .messageRouterHandler(RecentActivityMessageRouterHandler(project))
                .schemeHandlerFactory(RecentActivitySchemeHandlerFactory(project))
                .build()

            jCefComponent.getComponent()

        } else {
            JLabel("JCEF not supported")
        }
    }

    override fun dispose() {
        jCefComponent.dispose()
    }
}