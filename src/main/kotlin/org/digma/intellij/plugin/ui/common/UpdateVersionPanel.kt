package org.digma.intellij.plugin.ui.common

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.model.rest.version.BackendDeploymentType
import org.digma.intellij.plugin.service.EditorService
import org.digma.intellij.plugin.ui.common.Links.DIGMA_DOCKER_APP_URL
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import org.digma.intellij.plugin.updates.UpdateState
import org.digma.intellij.plugin.updates.UpdatesService
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.file.Path
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants


class UpdateVersionPanel(
    val project: Project
) : DigmaResettablePanel(), Disposable {

    private val logger: Logger = Logger.getInstance(UpdateVersionPanel::class.java)

    private val updatesService: UpdatesService = UpdatesService.getInstance(project)
    private val updateGuideDockerCompose: VirtualFile = loadFile("guides/upgrade_docker_compose.md")
    private val updateGuideForHelm: VirtualFile = loadFile("guides/upgrade_helm.md")

    init {
        updatesService.affectedPanel = this

        isOpaque = false
        layout = BoxLayout(this, BoxLayout.X_AXIS)

        isVisible = false

        rebuildInBackground()
    }

    companion object {

        protected val tempDirForGuides = FileUtil.createTempDirectory("digma_", "_guides", true)

        fun loadFile(resourceName: String): VirtualFile {
            val resourceInputStream = UpdateVersionPanel.javaClass.classLoader.getResourceAsStream(resourceName)
            val ioFile = Path.of(tempDirForGuides.absolutePath, resourceName).toFile()
            FileUtil.createParentDirs(ioFile)

            resourceInputStream.use { input ->
                ioFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            return LocalFileSystem.getInstance().findFileByIoFile(ioFile)!!
        }

    }

    override fun reset() {
        rebuildInBackground()
    }

    fun rebuildInBackground() {

        ApplicationManager.getApplication().invokeLater {

            removeExistingComponentsIfPresent()

            val updateState = updatesService.evalAndGetState()
            if (updateState.shouldUpdateAny()) {
                buildItemsInPanel(updateState)
                isVisible = true
            } else {
                isVisible = false
            }
        }
    }

    private fun removeExistingComponentsIfPresent() {
        if (components.isNotEmpty()) {
            this.components.forEach {
                this.remove(it)
            }
        }
    }

    fun buildItemsInPanel(updateState: UpdateState) {
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
        contentPanel.add(Box.createHorizontalStrut(5));
        contentPanel.add(icon)
        contentPanel.add(Box.createHorizontalStrut(5));
        val updateTextLabel = JLabel(buildText(updateState))
        contentPanel.add(updateTextLabel)
        contentPanel.add(Box.createHorizontalStrut(5));
        val updateButton = UpdateActionButton()
        contentPanel.add(updateButton)
        contentPanel.add(Box.createHorizontalStrut(5));

        updateButton.addActionListener {
            // the update action itself
            if (updateState.shouldUpdateBackend) {
                when (updateState.backendDeploymentType) {
                    BackendDeploymentType.Helm -> {
                        EditorService.getInstance(project).openVirtualFile(updateGuideForHelm, true)
                    }

                    BackendDeploymentType.DockerCompose -> {
                        EditorService.getInstance(project).openVirtualFile(updateGuideDockerCompose, true)
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
            updatesService.updateButtonClicked()
            this.isVisible = false
        }

        borderedPanel.add(contentPanel)
        borderedPanel.add(Box.createVerticalStrut(2))

        this.add(borderedPanel)
    }

    fun buildText(updateState: UpdateState): String {
        val txt: String
        if (updateState.shouldUpdateBackend) {
            txt = asHtml("<b>Update Recommended |</b> Digma analysis backend")
        } else {
            txt = asHtml("<b>Update Recommended |</b> Digma IDE plugin")
        }
        return txt
    }

    private fun getIcon(): Icon {
        return if (JBColor.isBright()) {
            Laf.Icons.Common.UpdateProductLight
        } else {
            Laf.Icons.Common.UpdateProductDark
        }
    }

    override fun dispose() {

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
