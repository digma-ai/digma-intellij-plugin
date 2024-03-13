package org.digma.intellij.plugin.ui.mainapp

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.insights.InsightsService
import org.digma.intellij.plugin.ui.jcef.DownloadHandlerAdapter
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.list.listBackground
import org.digma.intellij.plugin.ui.panels.DisposablePanel
import org.digma.intellij.plugin.ui.tests.TestsUpdater
import java.awt.BorderLayout
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JLabel

class MainAppPanel(private val project: Project) : DisposablePanel() {

    private var jCefComponent: JCefComponent? = null

    init {

        jCefComponent = createJcefComponent()

        val jcefUiComponent: JComponent = jCefComponent?.getComponent() ?: JLabel("JCEF not supported")

        layout = BorderLayout()
        border = JBUI.Borders.empty()
        add(jcefUiComponent, BorderLayout.CENTER)
        background = listBackground()

        Disposer.register(MainAppService.getInstance(project)) {
            dispose()
        }

        jCefComponent?.let {
            MainAppService.getInstance(project).setJCefComponent(it)
            project.service<TestsUpdater>().setJCefComponent(it)
            InsightsService.getInstance(project).setJCefComponent(it)
        }

    }

    private fun createJcefComponent(): JCefComponent? {
        return if (JBCefApp.isSupported()) {
            JCefComponent.JCefComponentBuilder(project, MainAppService.getInstance(project))
                .url(MAIN_APP_URL)
                .addMessageRouterHandler(MainAppMessageRouterHandler(project))
                .schemeHandlerFactory(MainAppSchemeHandlerFactory(project))
                .withDownloadAdapter(DownloadHandlerAdapter())
                .build()
        } else {
            null
        }
    }


    override fun getInsets(): Insets {
        return JBUI.emptyInsets()
    }

    override fun dispose() {
        jCefComponent?.dispose()
    }
}