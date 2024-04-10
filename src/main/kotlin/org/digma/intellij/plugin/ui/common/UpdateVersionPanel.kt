package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.util.Alarm
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import org.digma.intellij.plugin.updates.AggressiveUpdateService
import org.digma.intellij.plugin.updates.UpdateState
import org.digma.intellij.plugin.updates.UpdatesService
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel


class UpdateVersionPanel(
    val project: Project,
    //todo: tempEnableReset is temporary to disable reset. if this panel resets it causes change card on
    // UpdateIntellijNotificationWrapper , remove when we stop the support for UpdateIntellijNotificationWrapper.
    // please see comment on UpdateIntellijNotificationWrapper
    var tempEnableReset: Boolean = false
) : DigmaResettablePanel() {

    private val logger: Logger = Logger.getInstance(this::class.java)

    private var updateState = UpdatesService.getInstance(project).evalAndGetState()

    private val updateTextProperty = AtomicProperty("")

    private val myActionAlarm = Alarm()

    init {
        UpdatesService.getInstance(project).affectedPanel = this
        isOpaque = false
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        isVisible = false
        buildItemsInPanel()
        changeState()
    }

    private fun changeState() {
        //don't show if in update mode. will maybe show the next time
        if (AggressiveUpdateService.getInstance().isInUpdateMode()) {
            return
        }

        updateTextProperty.set(buildText(updateState))
        isVisible = updateState.shouldUpdateAny()
        Log.log(logger::debug, "state changed , isVisible={}, text={}", isVisible, updateTextProperty.get())
    }


    override fun reset() {
        if (tempEnableReset) {
            updateState = UpdatesService.getInstance(project).evalAndGetState()
            Log.log(logger::debug, "resetting panel, update state {}", updateState)
            changeState()
        }
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
        updateTextProperty.afterChange(UpdatesService.getInstance(project)) {
            updateTextLabel.text = updateTextProperty.get()
        }

        contentPanel.add(updateTextLabel)
        contentPanel.add(Box.createHorizontalStrut(5))
        val updateButton = UpdateActionButton()
        contentPanel.add(updateButton)
        contentPanel.add(Box.createHorizontalStrut(5))

        updateButton.addActionListener {

            myActionAlarm.cancelAllRequests()
            myActionAlarm.addRequest({
                ActivityMonitor.getInstance(project).registerUserAction(
                    "update button clicked",
                    mapOf(
                        "shouldUpdateBackend" to updateState.shouldUpdateBackend,
                        "shouldUpdatePlugin" to updateState.shouldUpdatePlugin,
                        "backendDeploymentType" to updateState.backendDeploymentType
                    )
                )


                // the update action itself
                if (updateState.shouldUpdateBackend) {
                    UpdateBackendAction().updateBackend(project, updateState.backendDeploymentType, updateButton)
                } else if (updateState.shouldUpdatePlugin) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, "Plugins")
                }

                this.isVisible = false
            }, 100)
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

