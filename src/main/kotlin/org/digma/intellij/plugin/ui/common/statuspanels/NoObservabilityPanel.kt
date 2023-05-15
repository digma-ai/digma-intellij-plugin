package org.digma.intellij.plugin.ui.common.statuspanels

import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JBUI.emptyInsets
import org.digma.intellij.plugin.notifications.NotificationUtil
import org.digma.intellij.plugin.ui.common.CopyableLabelHtml
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.MethodInstrumentationPresenter
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.boldFonts
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.model.insights.InsightsModel
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

fun createNoObservabilityPanel(project: Project, insightsModel: InsightsModel): DigmaResettablePanel {

    val model = MethodInstrumentationPresenter(project)

    val panel = JPanel(GridBagLayout())

    val constraints = GridBagConstraints()

    constraints.gridx = 1
    constraints.gridy = 1
    constraints.gridwidth = 1
    constraints.gridheight = 1
    constraints.anchor = GridBagConstraints.CENTER
    constraints.insets = JBUI.insets(10, 5)
    val icon = JLabel(getNoObservabilityIcon())
    icon.horizontalAlignment = SwingConstants.CENTER
    panel.add(icon, constraints)

    constraints.gridy = 2
    val noObservability = JLabel("No Observability")
    boldFonts(noObservability)
    noObservability.horizontalAlignment = SwingConstants.CENTER
    noObservability.horizontalTextPosition = SwingConstants.CENTER
    panel.add(noObservability, constraints)

    constraints.insets = emptyInsets()
    constraints.gridy = 3
    addNoObservabilityDetailsPart("Add an annotation to observe this ", panel, constraints)
    constraints.gridy = 4
    addNoObservabilityDetailsPart("method and collect data about ", panel, constraints)
    constraints.gridy = 5
    addNoObservabilityDetailsPart("its runtime behavior.", panel, constraints)

    constraints.gridy = 7
    constraints.fill = GridBagConstraints.BOTH
    constraints.insets = JBUI.insets(20, 5, 0, 5)
    val autoFixLabel1 = JLabel(asHtml("Before adding annotations, "))
    autoFixLabel1.horizontalAlignment = SwingConstants.CENTER
    autoFixLabel1.horizontalTextPosition = SwingConstants.CENTER
    panel.add(autoFixLabel1, constraints)

    constraints.gridy = 8
    constraints.anchor = GridBagConstraints.CENTER
    constraints.insets = emptyInsets()
    val autoFixLabel2 = JLabel(asHtml("please add the following dependency:"))
    autoFixLabel2.horizontalAlignment = SwingConstants.CENTER
    autoFixLabel2.horizontalTextPosition = SwingConstants.CENTER
    panel.add(autoFixLabel2, constraints)

    constraints.gridy = 9
    constraints.fill = GridBagConstraints.NONE
    constraints.anchor = GridBagConstraints.CENTER
    constraints.insets = emptyInsets()
    val missingDependencyLabel = CopyableLabelHtml(asHtml(""))
    missingDependencyLabel.isOpaque = true
    missingDependencyLabel.background = Laf.Colors.EDITOR_BACKGROUND
    panel.add(missingDependencyLabel, constraints)



    constraints.gridy = 10
    constraints.fill = GridBagConstraints.NONE
    constraints.anchor = GridBagConstraints.CENTER
    constraints.insets = JBUI.insets(10, 5)
    val addAnnotationButton = AddAnnotationButton()
    panel.add(addAnnotationButton, constraints)
    addAnnotationButton.addActionListener {
        val succeeded = model.instrumentMethod()
        if (succeeded) {
            (it.source as JButton).isEnabled = false
        } else {
            NotificationUtil.notifyError(project, "Failed to add annotation")
        }
    }



    panel.isOpaque = false
    panel.border = JBUI.Borders.empty()

    val resettablePanel = object : DigmaResettablePanel() {
        override fun reset() {
            val methodScope = insightsModel.scope as? MethodScope
            methodScope?.let {
                model.update(methodScope.getMethodInfo().id)
                if (model.canInstrumentMethod) {
                    addAnnotationButton.isEnabled = true
                    autoFixLabel1.isVisible = false
                    autoFixLabel2.isVisible = false
                    missingDependencyLabel.isVisible = false
                } else {
                    addAnnotationButton.isEnabled = false
                    autoFixLabel1.isVisible = model.cannotBecauseMissingDependency
                    autoFixLabel2.isVisible = model.cannotBecauseMissingDependency
                    missingDependencyLabel.isVisible = model.cannotBecauseMissingDependency
                    val depText = model.missingDependency ?: ""
                    missingDependencyLabel.text = asHtml(depText.replace(":", ":<br>"))
                }
            }

        }
    }
    resettablePanel.layout = BorderLayout()
    resettablePanel.add(panel, BorderLayout.CENTER)

    return resettablePanel
}


private fun addNoObservabilityDetailsPart(text: String, panel: JPanel, constraints: GridBagConstraints) {
    val noObservabilityDetailsLabel = JLabel(asHtml(text))
    noObservabilityDetailsLabel.horizontalAlignment = SwingConstants.CENTER
    noObservabilityDetailsLabel.horizontalTextPosition = SwingConstants.CENTER
    panel.add(noObservabilityDetailsLabel, constraints)
}

private fun getNoObservabilityIcon(): Icon {
    return if (JBColor.isBright()) {
        Laf.Icons.Common.NoObservabilityLight
    } else {
        Laf.Icons.Common.NoObservabilityDark
    }
}


private class AddAnnotationButton : JButton() {

    companion object {
        val bg = Laf.Colors.BUTTON_BACKGROUND
    }


    init {
        text = "Add Annotation"
        boldFonts(this)
        isContentAreaFilled = false
        horizontalAlignment = SwingConstants.CENTER
        background = bg
        isOpaque = true
        border = JBUI.Borders.empty(2)
        margin = emptyInsets()

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

