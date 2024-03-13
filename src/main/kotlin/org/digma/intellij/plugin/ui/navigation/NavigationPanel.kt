package org.digma.intellij.plugin.ui.navigation

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.jcef.DownloadHandlerAdapter
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.list.listBackground
import org.digma.intellij.plugin.ui.panels.DisposablePanel
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JLabel

class NavigationPanel(private val project: Project) : DisposablePanel() {

    private var jCefComponent: JCefComponent? = null

    init {

        jCefComponent = createJcefComponent()

        val jcefUiComponent: JComponent = jCefComponent?.getComponent() ?: JLabel("JCEF not supported")

        jcefUiComponent.preferredSize = Dimension(400, 112)

        layout = BorderLayout()
        border = JBUI.Borders.empty()
        add(jcefUiComponent, BorderLayout.CENTER)
        background = listBackground()

        Disposer.register(NavigationService.getInstance(project)) {
            dispose()
        }

        jCefComponent?.let {
            NavigationService.getInstance(project).setJCefComponent(it)
            CodeButtonCaretContextService.getInstance(project).setJCefComponent(it)
        }

    }


    private fun createJcefComponent(): JCefComponent? {
        return if (JBCefApp.isSupported()) {
            JCefComponent.JCefComponentBuilder(project, NavigationService.getInstance(project))
                .url(NAVIGATION_APP_URL)
                .addMessageRouterHandler(NavigationMessageRouterHandler(project))
                .schemeHandlerFactory(NavigationSchemeHandlerFactory(project))
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