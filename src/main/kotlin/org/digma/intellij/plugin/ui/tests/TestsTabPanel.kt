package org.digma.intellij.plugin.ui.tests

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.jcef.DownloadHandlerAdapter
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.jcef.JCefComponentBuilder
import org.digma.intellij.plugin.ui.list.listBackground
import org.digma.intellij.plugin.ui.panels.DisposablePanel
import org.digma.intellij.plugin.ui.service.TestsService
import java.awt.BorderLayout
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JLabel

class TestsTabPanel(project: Project) : DisposablePanel() {

    companion object {
        const val RUN_TEST_BUTTON_NAME: String = "run-test"
    }

    private var jCefComponent: JCefComponent? = null

    init {
        layout = BorderLayout()
        border = JBUI.Borders.empty()
        add(createComponent(project), BorderLayout.CENTER)
        background = listBackground()

        Disposer.register(project.service<TestsService>()) {
            dispose()
        }
    }

    private fun createComponent(project: Project): JComponent {

        jCefComponent = if (JBCefApp.isSupported()) {

            JCefComponentBuilder(project)
                .url(TESTS_APP_URL)
                .messageRouterHandler(TestsMessageRouterHandler(project))
                .schemeHandlerFactory(TestsSchemeHandlerFactory(project))
                .withParentDisposable(project.service<TestsService>())
                .withDownloadAdapter(DownloadHandlerAdapter())
                .build()

        } else {
            null
        }

        jCefComponent?.let {
            project.service<TestsUpdater>().setJCefComponent(it)
        }

        return jCefComponent?.getComponent() ?: JLabel("JCef is not supported")

    }

    override fun getInsets(): Insets {
        return JBUI.emptyInsets()
    }

    override fun dispose() {
        jCefComponent?.dispose()
    }
}