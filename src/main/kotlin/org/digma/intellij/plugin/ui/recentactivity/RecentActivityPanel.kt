package org.digma.intellij.plugin.ui.recentactivity

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.jcef.JCefComponent.JCefComponentBuilder
import org.digma.intellij.plugin.ui.list.listBackground
import org.digma.intellij.plugin.ui.panels.DisposablePanel
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JLabel

class RecentActivityPanel(private val project: Project) : DisposablePanel() {

    init {

        val jCefComponent = createJcefComponent()

        val jcefUiComponent: JComponent = jCefComponent?.getComponent() ?: JLabel("JCEF not supported")

        layout = BorderLayout()
        border = JBUI.Borders.empty()
        background = listBackground()
        add(jcefUiComponent, BorderLayout.CENTER)

        Disposer.register(project.service<RecentActivityService>()) {
            jCefComponent?.dispose()
            remove(jcefUiComponent)
            dispose()
        }

        jCefComponent?.let {
            project.service<RecentActivityService>().setJcefComponent(it)
            project.service<RecentActivityUpdater>().setJcefComponent(it)
            project.service<LiveViewUpdater>().setJcefComponent(it)
        }
    }


    private fun createJcefComponent(): JCefComponent? {
        return if (JBCefApp.isSupported()) {
            JCefComponentBuilder(
                project, "RecentActivity", project.service<RecentActivityService>(),
                RECENT_ACTIVITY_URL,
                RecentActivityMessageRouterHandler(project)
            )
                .build()
        } else {
            null
        }
    }


    override fun dispose() {
        //nothing to do
    }
}