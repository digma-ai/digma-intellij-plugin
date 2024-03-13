package org.digma.intellij.plugin.ui.assets

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.jcef.DownloadHandlerAdapter
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.list.listBackground
import org.digma.intellij.plugin.ui.panels.DisposablePanel
import java.awt.BorderLayout
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JLabel

class AssetsPanel(private val project: Project) : DisposablePanel() {

    private var jCefComponent: JCefComponent? = null

    init {

        jCefComponent = createJcefComponent()

        val jcefUiComponent: JComponent = jCefComponent?.getComponent() ?: JLabel("JCEF not supported")

        layout = BorderLayout()
        border = JBUI.Borders.empty()
        add(jcefUiComponent, BorderLayout.CENTER)
        background = listBackground()

        Disposer.register(AssetsService.getInstance(project)) {
            dispose()
        }
    }


    private fun createJcefComponent(): JCefComponent? {
        return if (JBCefApp.isSupported()) {
            JCefComponent.JCefComponentBuilder(project, AssetsService.getInstance(project))
                .url(ASSETS_APP_URL)
                .addMessageRouterHandler(AssetsMessageRouterHandler(project))
                .schemeHandlerFactory(AssetsSchemeHandlerFactory(project))
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