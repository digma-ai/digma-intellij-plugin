package org.digma.intellij.plugin.ui.common

import com.intellij.codeInsight.hint.HintManager
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.docker.DockerService
import org.digma.intellij.plugin.model.rest.version.BackendDeploymentType
import org.digma.intellij.plugin.service.EditorService
import org.digma.intellij.plugin.ui.common.Links.DIGMA_DOCKER_APP_URL
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import org.digma.intellij.plugin.updates.UpdateState
import org.digma.intellij.plugin.updates.UpdatesService
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants


const val UPDATE_GUIDE_DOCKER_COMPOSE_PATH = "/guides/upgrade_docker_compose.md"
const val UPDATE_GUIDE_DOCKER_COMPOSE_NAME = "upgrade_docker_compose.md"
const val UPDATE_GUIDE_HELM_PATH = "/guides/upgrade_helm.md"
const val UPDATE_GUIDE_HELM_NAME = "upgrade_helm.md"

class UpdateVersionPanel(
    val project: Project
) : DigmaResettablePanel() {

    private var updateState = project.service<UpdatesService>().evalAndGetState()

    private val updateTextProperty = AtomicProperty("")

    init {
        project.service<UpdatesService>().affectedPanel = this
        isOpaque = false
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        isVisible = false
        buildItemsInPanel()
        changeState()
    }

    private fun changeState() {
        updateTextProperty.set(buildText(updateState))
        isVisible = updateState.shouldUpdateAny()
    }


    override fun reset() {
        updateState = project.service<UpdatesService>().evalAndGetState()
        changeState()
    }


    private fun buildItemsInPanel() {
        val borderedPanel = JPanel()
        borderedPanel.layout = BoxLayout(borderedPanel, BoxLayout.Y_AXIS)
        borderedPanel.isOpaque = true
        borderedPanel.border = BorderFactory.createLineBorder(Laf.Colors.BLUE_LIGHT_SHADE, 1)

        borderedPanel.add(Box.createVerticalStrut(2))

        val contentPanel = JPanel()
        contentPanel.layout = BoxLayout(contentPanel, BoxLayout.X_AXIS)
        contentPanel.background = Laf.Colors.EDITOR_BACKGROUND
        contentPanel.isOpaque = true
        contentPanel.border = JBUI.Borders.empty(4)

        val icon = JLabel(getIcon())
        contentPanel.add(Box.createHorizontalStrut(5))
        contentPanel.add(icon)
        contentPanel.add(Box.createHorizontalStrut(5))
        val updateTextLabel = JLabel(updateTextProperty.get())
        updateTextProperty.afterChange(project.service<UpdatesService>()) {
            updateTextLabel.text = updateTextProperty.get()
        }

        contentPanel.add(updateTextLabel)
        contentPanel.add(Box.createHorizontalStrut(5))
        val updateButton = UpdateActionButton()
        contentPanel.add(updateButton)
        contentPanel.add(Box.createHorizontalStrut(5))

        updateButton.addActionListener {
            // the update action itself
            if (updateState.shouldUpdateBackend) {
                when (updateState.backendDeploymentType) {
                    BackendDeploymentType.Helm -> {
                        EditorService.getInstance(project).openClasspathResourceReadOnly(UPDATE_GUIDE_HELM_NAME,UPDATE_GUIDE_HELM_PATH)
                    }

                    BackendDeploymentType.DockerCompose -> {
                        val upgradePopupLabel = JLabel(asHtml("<p>" +
                                "<b>The Digma local engine is being updated</b>" +
                                "</p><p>This can take a few minutes in which Digma may be offline</p>"))
                        upgradePopupLabel.border = JBUI.Borders.empty(3)
                        HintManager.getInstance().showHint(upgradePopupLabel, RelativePoint.getNorthWestOf(updateButton), HintManager.HIDE_BY_ESCAPE, 3000)
                        project.service<DockerService>().upgradeEngine(project);
                        //EditorService.getInstance(project).openClasspathResourceReadOnly(UPDATE_GUIDE_DOCKER_COMPOSE_NAME,UPDATE_GUIDE_DOCKER_COMPOSE_PATH)
                    }

                    BackendDeploymentType.DockerExtension -> {
                        BrowserUtil.browse(DIGMA_DOCKER_APP_URL, project)
                    }

                    else -> {
                        // default fallback to Docker Extension
                        BrowserUtil.browse(DIGMA_DOCKER_APP_URL, project)
                    }
                }
            } else if (updateState.shouldUpdatePlugin) {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, "Plugins")
            } else {
                // very unlikely, since currently support update of backend and/or plugin
            }

            // post click
            project.service<UpdatesService>().updateButtonClicked()
            this.isVisible = false
        }

        borderedPanel.add(contentPanel)
        borderedPanel.add(Box.createVerticalStrut(2))

        this.add(borderedPanel)
    }

    private fun buildText(updateState: UpdateState): String {
        return if (updateState.shouldUpdateBackend) {
            asHtml("<b>Update Recommended |</b> Digma analysis backend")
        } else {
            asHtml("<b>Update Recommended |</b> Digma IDE plugin")
        }
    }

    private fun getIcon(): Icon {
        return if (JBColor.isBright()) {
            Laf.Icons.Common.UpdateProductLight
        } else {
            Laf.Icons.Common.UpdateProductDark
        }
    }

}

private class UpdateActionButton : JButton() {

    companion object {
        val bg = Laf.Colors.BUTTON_BACKGROUND
    }


    init {
        text = "Update"
        boldFonts(this)
        isContentAreaFilled = false
        horizontalAlignment = SwingConstants.CENTER
        background = bg
        isOpaque = true
        border = JBUI.Borders.empty(2)
        margin = JBUI.emptyInsets()

        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                if (!isEnabled) return
                border = JBUI.Borders.customLine(JBColor.GRAY, 2)
            }

            override fun mouseExited(e: MouseEvent?) {
                border = JBUI.Borders.empty(2)
            }

            override fun mousePressed(e: MouseEvent?) {
                if (!isEnabled) return
                background = JBColor.BLUE
            }

            override fun mouseReleased(e: MouseEvent?) {
                if (!isEnabled) return
                background = bg
            }
        })

    }

    override fun setEnabled(b: Boolean) {
        super.setEnabled(b)
        if (b) {
            background = bg
            isBorderPainted = true
        } else {
            background = JBColor.LIGHT_GRAY
            isBorderPainted = false
        }
    }
}
