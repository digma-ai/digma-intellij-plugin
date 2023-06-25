package org.digma.intellij.plugin.ui.frameworks

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.util.Alarm
import com.intellij.util.AlarmFactory
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.idea.frameworks.QuarkusConfigureDepsService
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.buildLinkTextWithUnderLine
import org.digma.intellij.plugin.ui.common.spanBold
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel

class QuarkusConfigureDepsPanel(
    val project: Project,
) : DigmaResettablePanel(), Disposable {

    init {
        theService().affectedPanel = this

        isOpaque = false
        layout = BoxLayout(this, BoxLayout.X_AXIS)

        isVisible = false

        buildItemsInPanel()

        checkStateAndUpdateUi()
    }

    private fun theService(): QuarkusConfigureDepsService {
        return project.service<QuarkusConfigureDepsService>()
    }

    private fun checkStateAndUpdateUi() {
        val shouldDisplayPanel = theService().shouldDisplayPanel()
        isVisible = shouldDisplayPanel
    }

    override fun reset() {
        checkStateAndUpdateUi()
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

        val icon = JLabel(Laf.Icons.Misc.Quarkus)
        contentPanel.add(Box.createHorizontalStrut(5));
        contentPanel.add(icon)
        contentPanel.add(Box.createHorizontalStrut(5));
        val updateTextLabel = JLabel(asHtml(spanBold("Configure Quarkus to use Digma | ")))
        contentPanel.add(updateTextLabel)
        contentPanel.add(Box.createHorizontalStrut(5))
        val depsButton = QuarkusDependenciesButton(theService())
        contentPanel.add(depsButton)
        contentPanel.add(Box.createHorizontalStrut(5));

        borderedPanel.add(contentPanel)
        borderedPanel.add(Box.createVerticalStrut(2))

        this.add(borderedPanel)
    }

    override fun dispose() {

    }
}

private class QuarkusDependenciesButton(service: QuarkusConfigureDepsService) :
    ActionLink("Click here", null) {

    private val clickAlarm: Alarm

    init {
        autoHideOnDisable = false
        setText(asHtml(buildLinkTextWithUnderLine(text)))

        clickAlarm = AlarmFactory.getInstance().create()

        val actionDef: () -> Unit = {
            service.buttonClicked()
        }

        addActionListener {
            clickAlarm.cancelAllRequests()
            clickAlarm.addRequest(actionDef, 250)
        }
    }

}
