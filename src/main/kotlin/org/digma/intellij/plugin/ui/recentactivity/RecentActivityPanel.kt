package org.digma.intellij.plugin.ui.recentactivity

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.reload.ReloadService
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.jcef.JCefComponent.JCefComponentBuilder
import org.digma.intellij.plugin.ui.list.listBackground
import org.digma.intellij.plugin.ui.panels.DisposablePanel
import org.digma.intellij.plugin.ui.panels.ReloadablePanel
import java.awt.BorderLayout
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JLabel

class RecentActivityPanel(private val project: Project) : DisposablePanel(), ReloadablePanel {

    private var jCefComponent: JCefComponent? = null

    private var parentDisposable = Disposer.newDisposable()

    init {
        jCefComponent = build()
        service<ReloadService>().register(this, project.service<RecentActivityService>())
        Disposer.register(project.service<RecentActivityService>()) {
            dispose()
        }
    }


    private fun build(): JCefComponent? {

        val jCefComponent = createJcefComponent()

        val jcefUiComponent: JComponent = jCefComponent?.getComponent() ?: JLabel("JCEF not supported")

        layout = BorderLayout()
        border = JBUI.Borders.empty()
        add(jcefUiComponent, BorderLayout.CENTER)
        background = listBackground()

        jCefComponent?.let {
            project.service<RecentActivityService>().setJcefComponent(it)
            project.service<RecentActivityUpdater>().setJcefComponent(it)
            project.service<LiveViewUpdater>().setJcefComponent(it)
        }

        return jCefComponent
    }


    override fun reload() {
        project.service<LiveViewUpdater>().stopLiveView()
        dispose()
        removeAll()
        parentDisposable = Disposer.newDisposable()
        jCefComponent = build()
    }


    private fun createJcefComponent(): JCefComponent? {
        return if (JBCefApp.isSupported()) {
            JCefComponentBuilder(
                project, "RecentActivity", parentDisposable,
                RECENT_ACTIVITY_URL,
                RecentActivityMessageRouterHandler(project)
            )
                .build()
        } else {
            null
        }
    }


    override fun getInsets(): Insets {
        return JBUI.emptyInsets()
    }

    override fun dispose() {
        Disposer.dispose(parentDisposable)
        jCefComponent?.let {
            Disposer.dispose(it)
        }
    }
}