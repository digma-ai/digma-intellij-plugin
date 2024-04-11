package org.digma.intellij.plugin.ui.common.statuspanels

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.AncestorListenerAdapter
import com.intellij.util.AlarmFactory
import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.UpdateActionButton
import org.digma.intellij.plugin.ui.common.UpdateBackendAction
import org.digma.intellij.plugin.ui.list.listBackground
import org.digma.intellij.plugin.updates.AggressiveUpdateService
import org.digma.intellij.plugin.updates.AggressiveUpdateStateChangedEvent
import org.digma.intellij.plugin.updates.CurrentUpdateState
import org.digma.intellij.plugin.updates.PublicUpdateState
import java.awt.Cursor
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.SwingConstants
import javax.swing.event.AncestorEvent


fun createAggressiveUpdatePanel(project: Project, parentDisposable: Disposable, source: String): JPanel {

    val mainPanel = JPanel(GridBagLayout())
    mainPanel.isOpaque = false
    mainPanel.border = empty()
    mainPanel.background = listBackground()

    val htmlText = getUpdateBackendMessageHtml()
    val textPane = createTextPaneWithHtml(htmlText)
    val messagePanel = createCommonEmptyStatePanelWIthIconAndTextPane(getDigmaIcon(), textPane)
    messagePanel.background = listBackground()


    val constraints = GridBagConstraints()
    constraints.gridx = 1
    constraints.gridy = 1
    constraints.ipady = 20
    mainPanel.add(messagePanel, constraints)


    constraints.gridy = 2
    constraints.ipady = 20

    val updateButton = UpdateActionButton()
    updateButton.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    val updateClickAlarm = AlarmFactory.getInstance().create()
    updateButton.addActionListener {

        //prevent double click
        updateClickAlarm.cancelAllRequests()
        updateClickAlarm.addRequest({

            val updateState = AggressiveUpdateService.getInstance().getUpdateState()

            ActivityMonitor.getInstance(project).registerUserAction(
                "force update button clicked", mapOf(
                    "update mode" to updateState.updateState,
                    "source" to "$source tool window"
                )
            )

            when (updateState.updateState) {
                CurrentUpdateState.UPDATE_BACKEND -> updateBackend(project, updateButton)
                CurrentUpdateState.UPDATE_PLUGIN -> updatePlugin(project)
                CurrentUpdateState.UPDATE_BOTH -> updateBackend(project, updateButton)
                CurrentUpdateState.OK -> {/* todo: should not be here : send error report */
                }
            }

        }, 200)
    }

    updateButton.horizontalAlignment = SwingConstants.CENTER
    updateButton.horizontalTextPosition = SwingConstants.CENTER
    updateButton.border = empty()
    mainPanel.add(updateButton, constraints)

    constraints.gridy = 3
    val slackPanel = createSlackLinkPanel(project)
    slackPanel.background = listBackground()
    mainPanel.add(slackPanel, constraints)


    mainPanel.addAncestorListener(object : AncestorListenerAdapter() {
        override fun ancestorAdded(event: AncestorEvent?) {
            val updateState = AggressiveUpdateService.getInstance().getUpdateState()
            changeText(updateState, textPane)
        }
    })



    ApplicationManager.getApplication().messageBus.connect(parentDisposable)
        .subscribe(AggressiveUpdateStateChangedEvent.UPDATE_STATE_CHANGED_TOPIC, object : AggressiveUpdateStateChangedEvent {
            override fun stateChanged(updateState: PublicUpdateState) {
                EDT.ensureEDT {
                    changeText(updateState, textPane)
                }
            }
        })

    return wrapWithScrollable(mainPanel)
}


private fun changeText(updateState: PublicUpdateState, textPane: JTextPane) {
    when (updateState.updateState) {
        CurrentUpdateState.UPDATE_BACKEND -> textPane.text = getUpdateBackendMessageHtml()
        CurrentUpdateState.UPDATE_PLUGIN -> textPane.text = getUpdatePluginMessageHtml()
        CurrentUpdateState.UPDATE_BOTH -> textPane.text = getUpdateBothMessageHtml()
        CurrentUpdateState.OK -> textPane.text = "This message should not be sown,something is wrong"
    }
}


private fun updatePlugin(project: Project) {
    ShowSettingsUtil.getInstance().showSettingsDialog(project, "Plugins")
}

private fun updateBackend(project: Project, updateButton: UpdateActionButton) {
    UpdateBackendAction().updateBackend(project, AggressiveUpdateService.getInstance().getUpdateState().backendDeploymentType, updateButton)
}


private fun getUpdateBackendMessageHtml(): String {
    return getMessageHtml("Update Required", "Your Digma Engine is too old.<br>Please update to use the latest features.")
}

private fun getUpdatePluginMessageHtml(): String {
    return getMessageHtml("Plugin Update Required", "This version of the Digma plugin is too old.<br>Please update to use the latest features.")
}

private fun getUpdateBothMessageHtml(): String {
    return getMessageHtml(
        "Plugin/Backend Update Required",
        "This version of the Digma plugin/backend is too old.<br>Please update to use the latest features"
    )
}


private fun getMessageHtml(title: String, paragraph: String): String {

    return "<html>" +
            "<head>" +
            "<style>" +
            "h3 {text-align: center;}" +
            "p {text-align: center;}" +
            "div {text-align: center;}" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<h3>$title</h3>" +
            "<p>$paragraph</p>" +
            "</body>" +
            "</html>"
}


private fun getDigmaIcon(): Icon {
    return Laf.Icons.Common.DigmaLogoPng
}



