package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.assets.AssetsPanel
import java.awt.Component
import javax.swing.BoxLayout
import javax.swing.JPanel


private const val ASSETS_TAB_NAME = "Assets"

class HomePanel(project: Project) : JPanel() {


    private val tabbedPane = JBTabbedPane()


    init {

        isOpaque = false
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = JBUI.Borders.empty()

        add(tabbedPane)
        tabbedPane.isOpaque = false
        tabbedPane.border = JBUI.Borders.empty()

        val assetsPanel = createAssetsPanel(project);
        tabbedPane.addTab(ASSETS_TAB_NAME, assetsPanel)
    }


    private fun createAssetsPanel(project: Project): Component {
        return AssetsPanel(project)
    }

}