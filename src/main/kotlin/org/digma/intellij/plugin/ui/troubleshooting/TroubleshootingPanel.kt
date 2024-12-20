package org.digma.intellij.plugin.ui.troubleshooting

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.reload.ReloadObserver
import org.digma.intellij.plugin.reload.ReloadService
import org.digma.intellij.plugin.reload.ReloadableJCefContainer
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.list.listBackground
import org.digma.intellij.plugin.ui.panels.DisposablePanel
import java.awt.BorderLayout
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JLabel

class TroubleshootingPanel(private val project: Project) : DisposablePanel(), ReloadableJCefContainer {

    private var jCefComponent: JCefComponent? = null

    private var parentDisposable = Disposer.newDisposable()

    init {
        jCefComponent = build()
        jCefComponent?.let {
            service<ReloadService>().register(this, parentDisposable)
            service<ReloadObserver>().register(project, "Troubleshooting", it.getComponent(), parentDisposable)
        }
        Disposer.register(TroubleshootingService.getInstance(project)) {
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

        return jCefComponent
    }

    private fun createJcefComponent(): JCefComponent? {
        return if (JBCefApp.isSupported()) {
            JCefComponent.JCefComponentBuilder(
                project, "Troubleshooting", parentDisposable,
                TROUBLESHOOTING_URL,
                TroubleshootingMessageRouterHandler(project)
            ).build()
        } else {
            null
        }
    }


    override fun reload() {
        dispose()
        removeAll()
        parentDisposable = Disposer.newDisposable()
        jCefComponent = build()
        jCefComponent?.let {
            service<ReloadService>().register(this, parentDisposable)
            service<ReloadObserver>().register(project, "Troubleshooting", it.getComponent(), parentDisposable)
        }
    }

    override fun getProject(): Project {
        return project
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