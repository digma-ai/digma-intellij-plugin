package org.digma.intellij.plugin.ui.common.statuspanels

import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JBUI.emptyInsets
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.notifications.NotificationUtil
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.instrumentation.MethodInstrumentationPresenter
import org.digma.intellij.plugin.ui.common.OtelDependencyButton
import org.digma.intellij.plugin.ui.common.Text
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.boldFonts
import org.digma.intellij.plugin.ui.common.span
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
import javax.swing.ScrollPaneConstants
import javax.swing.SwingConstants

fun createNoObservabilityPanel(project: Project, insightsModel: InsightsModel): DigmaResettablePanel {

    val model = MethodInstrumentationPresenter(project)
    val propertyOfMissing = AtomicProperty("unknown")

    val componentsPanel = JPanel(GridBagLayout())
    componentsPanel.isOpaque = false
    componentsPanel.border = JBUI.Borders.empty()

    val constraints = GridBagConstraints()

    constraints.gridx = 1
    constraints.gridy = 1
    constraints.gridwidth = 1
    constraints.gridheight = 1
    constraints.anchor = GridBagConstraints.CENTER
    constraints.insets = JBUI.insets(10, 5)
    val icon = JLabel(getNoObservabilityIcon())
    icon.horizontalAlignment = SwingConstants.CENTER
    componentsPanel.add(icon, constraints)

    constraints.insets = emptyInsets()
    constraints.gridy = 3
    val mainMessageTextPane = createTextPaneWithHtmlTitleAndParagraph("No Observability","Add an annotation to observe this<br>method and collect data about<br>its runtime behavior.")
    val mainMessagePanel = JPanel(BorderLayout())
    mainMessagePanel.isOpaque = false
    mainMessagePanel.border = JBUI.Borders.empty()
    mainMessagePanel.add(mainMessageTextPane, BorderLayout.CENTER)
    componentsPanel.add(mainMessagePanel, constraints)

    constraints.gridy = 4
    constraints.fill = GridBagConstraints.BOTH
    constraints.insets = JBUI.insets(20, 5, 0, 5)
    val autoFixPanel = JPanel(BorderLayout())

    val autoFixLabel = JLabel()
    autoFixLabel.border = JBUI.Borders.emptyRight(10)
    autoFixPanel.add(autoFixLabel, BorderLayout.CENTER)
    propertyOfMissing.afterChange {
        autoFixLabel.text = asHtml(span(Laf.Colors.RED_OF_MISSING, "missing dependency: $it"))
    }

    val autoFixLink = OtelDependencyButton("Autofix", project, model)
    autoFixPanel.add(autoFixLink, BorderLayout.EAST)

    val workingOnItLabel = JLabel(asHtml(Text.NO_OBSERVABILITY_WORKING_ON_IT_DESCRIPTION))
    workingOnItLabel.isVisible = false
    workingOnItLabel.border = JBUI.Borders.emptyTop(10)
    autoFixPanel.add(workingOnItLabel, BorderLayout.SOUTH)

    componentsPanel.add(autoFixPanel, constraints)

    constraints.gridy = 5
    constraints.fill = GridBagConstraints.NONE
    constraints.anchor = GridBagConstraints.CENTER
    constraints.insets = JBUI.insets(10, 5)
    val addAnnotationButton = AddAnnotationButton()
    componentsPanel.add(addAnnotationButton, constraints)
    addAnnotationButton.addActionListener {
        val succeeded = model.instrumentMethod()
        if (succeeded) {
            (it.source as JButton).isEnabled = false
        } else {
            NotificationUtil.notifyError(project, "Failed to add annotation")
        }
    }


    val resettablePanel = object : DigmaResettablePanel() {
        override fun reset() {
            val methodScope = insightsModel.scope as? MethodScope
            methodScope?.let {

                Backgroundable.executeOnPooledThread {
                    model.update(methodScope.getMethodInfo().id)

                    EDT.ensureEDT{
                        propertyOfMissing.set(model.missingDependency ?: "unknown")
                        if (model.canInstrumentMethod) {
                            addAnnotationButton.isEnabled = true
                            autoFixPanel.isVisible = false
                        } else {
                            addAnnotationButton.isEnabled = false
                            autoFixPanel.isVisible = model.cannotBecauseMissingDependency
                        }
                    }
                }

            }
        }
    }


    val scrollPane = JBScrollPane()
    scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    scrollPane.setViewportView(componentsPanel)
    scrollPane.border = JBUI.Borders.empty()
    scrollPane.isOpaque = false


    resettablePanel.layout = BorderLayout()
    resettablePanel.add(scrollPane, BorderLayout.CENTER)
    resettablePanel.isOpaque = false
    resettablePanel.border = JBUI.Borders.empty()
    autoFixLink.defineTheAction(resettablePanel, workingOnItLabel)

    return resettablePanel
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

