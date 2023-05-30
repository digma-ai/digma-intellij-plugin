package org.digma.intellij.plugin.ui.common.statuspanels

import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JBUI.emptyInsets
import org.digma.intellij.plugin.notifications.NotificationUtil
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
import javax.swing.ScrollPaneConstants
import javax.swing.SwingConstants

fun createNoObservabilityPanel(project: Project, insightsModel: InsightsModel): DigmaResettablePanel {

    val model = MethodInstrumentationPresenter(project)

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
    mainMessagePanel.add(mainMessageTextPane,BorderLayout.CENTER)
    componentsPanel.add(mainMessagePanel,constraints)


    constraints.insets = emptyInsets()
    constraints.gridy = 4
    val autoFixMessageTextPane = createTextPaneWithHtmlParagraph("Before adding annotations,<br>please add the following dependency:")
    val autoFixMessagePanel = JPanel(BorderLayout())
    autoFixMessagePanel.isOpaque = false
    autoFixMessagePanel.border = JBUI.Borders.empty()
    autoFixMessagePanel.add(autoFixMessageTextPane,BorderLayout.CENTER)
    componentsPanel.add(autoFixMessagePanel,constraints)

    constraints.gridy = 5
    constraints.fill = GridBagConstraints.NONE
    constraints.anchor = GridBagConstraints.CENTER
    constraints.insets = emptyInsets()
    val missingDependencyLabel = JBLabel(asHtml(""))
    missingDependencyLabel.setCopyable(true)
    missingDependencyLabel.isOpaque = true
    missingDependencyLabel.background = Laf.Colors.EDITOR_BACKGROUND
    componentsPanel.add(missingDependencyLabel, constraints)



    constraints.gridy = 6
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
                model.update(methodScope.getMethodInfo().id)
                if (model.canInstrumentMethod) {
                    addAnnotationButton.isEnabled = true
                    autoFixMessagePanel.isVisible = false
                    missingDependencyLabel.isVisible = false
                } else {
                    addAnnotationButton.isEnabled = false
                    autoFixMessagePanel.isVisible = model.cannotBecauseMissingDependency
                    missingDependencyLabel.isVisible = model.cannotBecauseMissingDependency
                    val depText = model.missingDependency ?: ""
                    missingDependencyLabel.text = asHtml(depText)
                }
            }
        }
    }


    val scrollPane = JBScrollPane()
    scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    scrollPane.setViewportView(componentsPanel)

    resettablePanel.layout = BorderLayout()
    resettablePanel.add(scrollPane, BorderLayout.CENTER)

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

