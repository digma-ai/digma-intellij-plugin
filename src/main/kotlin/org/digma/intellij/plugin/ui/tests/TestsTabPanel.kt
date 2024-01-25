package org.digma.intellij.plugin.ui.tests

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.jcef.DownloadHandlerAdapter
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.jcef.JCefComponent.JCefComponentBuilder
import org.digma.intellij.plugin.ui.list.listBackground
import org.digma.intellij.plugin.ui.panels.DisposablePanel
import org.digma.intellij.plugin.ui.service.TestsService
import java.awt.BorderLayout
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JLabel

class TestsTabPanel(private val project: Project) : DisposablePanel() {

    companion object {
        const val RUN_TEST_BUTTON_NAME: String = "run-test"
    }

    private var jCefComponent: JCefComponent? = null

    init {

        jCefComponent = createJcefComponent()

        val jcefUiComponent: JComponent = jCefComponent?.getComponent() ?: JLabel("JCEF not supported")

        layout = BorderLayout()
        border = JBUI.Borders.empty()
        add(jcefUiComponent, BorderLayout.CENTER)
        background = listBackground()

        Disposer.register(project.service<TestsService>()) {
            dispose()
        }

        jCefComponent?.let {
            project.service<TestsUpdater>().setJCefComponent(it)
        }
    }


    private fun createJcefComponent(): JCefComponent? {
        return if (JBCefApp.isSupported()) {
            JCefComponentBuilder(project, project.service<TestsService>())
                .url(TESTS_APP_URL)
                .messageRouterHandler(TestsMessageRouterHandler(project))
                .schemeHandlerFactory(TestsSchemeHandlerFactory(project))
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