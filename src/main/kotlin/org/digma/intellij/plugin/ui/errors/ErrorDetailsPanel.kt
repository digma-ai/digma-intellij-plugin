package org.digma.intellij.plugin.ui.errors

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.ui.layout.PropertyBinding
import com.intellij.util.ui.JBUI.Borders
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.errordetails.CodeObjectErrorDetails
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError
import org.digma.intellij.plugin.service.ErrorsActionsService
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.createScorePanel
import org.digma.intellij.plugin.ui.list.ScrollablePanelList
import org.digma.intellij.plugin.ui.list.errordetails.ErrorFramesPanelList
import org.digma.intellij.plugin.ui.list.panelListBackground
import org.digma.intellij.plugin.ui.model.errors.ErrorsModel
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import org.ocpsoft.prettytime.PrettyTime
import java.awt.*
import java.util.*
import java.util.function.Consumer
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel


fun errorDetailsPanel(project: Project, errorsModel: ErrorsModel): DigmaTabPanel {

    val backButton = backButton(project)
    val namePanel = namePanel(errorsModel)
    val scorePanel = scorePanel(errorsModel)

    val titlePanel = JPanel()
    titlePanel.layout = BorderLayout(10, 10)
    titlePanel.border = Borders.empty(5, 2, 5, 2)
    titlePanel.isOpaque = false
    titlePanel.add(backButton, BorderLayout.WEST)
    titlePanel.add(namePanel, BorderLayout.CENTER)
    titlePanel.add(scorePanel, BorderLayout.EAST)
    val titlePanelWrapper = JPanel()
    titlePanelWrapper.layout = GridLayout(1, 1)
    titlePanelWrapper.isOpaque = false
    titlePanelWrapper.add(titlePanel)


    val servicesPanel = JPanel()
    servicesPanel.layout = FlowLayout(FlowLayout.LEFT, 5, 5)
    servicesPanel.isOpaque = false
    buildServicesPanel(servicesPanel, errorsModel)
    val affectedServicesLabel = JLabel("Affected Services")
    affectedServicesLabel.toolTipText = getAffectedServicesTooltip(errorsModel)
    val servicesPanelWrapper = JPanel()
    servicesPanelWrapper.layout = BorderLayout()
    servicesPanelWrapper.isOpaque = false
    servicesPanelWrapper.add(affectedServicesLabel, BorderLayout.NORTH)
    servicesPanelWrapper.add(servicesPanel, BorderLayout.CENTER)


    val timesPanel = timesPanel(errorsModel)
    val timesPanelWrapper = JPanel()
    timesPanelWrapper.layout = GridLayout(1, 1)
    timesPanelWrapper.isOpaque = false
    timesPanelWrapper.add(timesPanel)



    val framesList = ScrollablePanelList(ErrorFramesPanelList(project,
        errorsModel.errorDetails.flowStacks.getCurrentStack(), false))

    val flowStackNavigation = flowStackNavigation(errorsModel,framesList)



    val bottomPanel = bottomPanel(errorsModel,framesList)


    val result = object : DigmaTabPanel() {
        override fun getPreferredFocusableComponent(): JComponent {
            return backButton.getComponent(0) as JComponent
        }

        override fun getPreferredFocusedComponent(): JComponent {
            return backButton.getComponent(0) as JComponent
        }

        override fun reset() {
            namePanel.reset()
            scorePanel.reset()
            buildServicesPanel(servicesPanel, errorsModel)
            affectedServicesLabel.toolTipText = getAffectedServicesTooltip(errorsModel)
            servicesPanel.revalidate()
            servicesPanel.repaint()
            servicesPanelWrapper.revalidate()
            timesPanel.reset()
            flowStackNavigation.reset()
            framesList.getModel().setListData(errorsModel.errorDetails.flowStacks.getCurrentStack())


            //reconstruct services panel on reset
            val index = components.indexOf(servicesPanelWrapper)
            remove(servicesPanelWrapper)
            val constraints = GridBagConstraints()
            constraints.gridx = 0
            constraints.gridy = 1
            constraints.weightx = 1.0
            constraints.fill = GridBagConstraints.BOTH
            constraints.weighty = weightyForServicesPanel(errorsModel)
            add(servicesPanelWrapper, constraints, index)

            revalidate()
        }
    }




    result.layout = GridBagLayout()
    val constraints = GridBagConstraints()
    constraints.gridx = 0
    constraints.gridy = 0
    constraints.weightx = 1.0
    constraints.weighty = 0.0

    constraints.fill = GridBagConstraints.HORIZONTAL
    result.add(titlePanelWrapper, constraints)

    constraints.gridy = 1
    constraints.fill = GridBagConstraints.BOTH
    constraints.weighty = weightyForServicesPanel(errorsModel)
    result.add(servicesPanelWrapper, constraints)

    constraints.gridy = 2
    constraints.weighty = 0.0
    constraints.fill = GridBagConstraints.HORIZONTAL
    result.add(timesPanelWrapper, constraints)

    constraints.gridy = 3
    constraints.fill = GridBagConstraints.HORIZONTAL
    result.add(flowStackNavigation, constraints)

    constraints.gridy = 4
    constraints.weighty = 1.0
    constraints.fill = GridBagConstraints.BOTH
    result.add(framesList, constraints)

    constraints.gridy = 5
    constraints.weighty = 0.0
    constraints.fill = GridBagConstraints.HORIZONTAL
    result.add(bottomPanel, constraints)

    result.background = panelListBackground()
    return result

}



fun bottomPanel(errorsModel: ErrorsModel, framesList: ScrollablePanelList): JPanel {

    val result = panel {
        row {
            checkBox("Workspace only")
                .horizontalAlign(HorizontalAlign.LEFT).applyToComponent {
                    isOpaque = false
                    isContentAreaFilled = false
                    isBorderPainted = false
                    isSelected = errorsModel.errorDetails.flowStacks.isWorkspaceOnly
                    addActionListener(){
                        errorsModel.errorDetails.flowStacks.isWorkspaceOnly = isSelected
                        framesList.getModel().setListData(errorsModel.errorDetails.flowStacks.getCurrentStack())
                    }
                }

            link("Open raw trace"){

            }.horizontalAlign(HorizontalAlign.RIGHT)
        }.layout(RowLayout.INDEPENDENT)
    }

    result.isOpaque = false
    return result
}

fun weightyForServicesPanel(errorsModel: ErrorsModel): Double {
    return when (errorsModel.errorDetails.delegate?.originServices?.size) {
        1, 2 -> 0.0
        3, 4 -> 0.2
        5, 6 -> 0.4
        7, 8 -> 0.6
        else -> 1.0
    }
}

fun flowStackNavigation(errorsModel: ErrorsModel, framesList: ScrollablePanelList): DialogPanel {

    val currentLabel = JLabel("0/0 Flow Stacks")

    val backButton = JButton(Icons.BACK)
    backButton.isOpaque = false
    backButton.isContentAreaFilled = false
    backButton.isBorderPainted = false
    backButton.preferredSize = Dimension(48, 48)
    backButton.maximumSize = Dimension(48, 48)
    backButton.rolloverIcon = Icons.BACK_ROLLOVER
    backButton.addActionListener {
        val stackSize = errorsModel.errorDetails.flowStacks.stacks.size
        errorsModel.errorDetails.flowStacks.goBack()
        val currentStack = errorsModel.errorDetails.flowStacks.current.plus(1)
        currentLabel.text = "${currentStack}/${stackSize} Flow Stacks"
        framesList.getModel().setListData(errorsModel.errorDetails.flowStacks.getCurrentStack())
    }

    val forwardButton = JButton(Icons.FORWARD)
    forwardButton.isOpaque = false
    forwardButton.isContentAreaFilled = false
    forwardButton.isBorderPainted = false
    forwardButton.preferredSize = Dimension(48, 48)
    forwardButton.maximumSize = Dimension(48, 48)
    forwardButton.rolloverIcon = Icons.FORWARD_ROLLOVER
    forwardButton.addActionListener {
        val stackSize = errorsModel.errorDetails.flowStacks.stacks.size
        errorsModel.errorDetails.flowStacks.goForward()
        val currentStack = errorsModel.errorDetails.flowStacks.current.plus(1)
        currentLabel.text = "${currentStack}/${stackSize} Flow Stacks"
        framesList.getModel().setListData(errorsModel.errorDetails.flowStacks.getCurrentStack())
    }


    val panel = JPanel()
    panel.layout = GridBagLayout()
    panel.add(backButton)
    val currentStackLabelConstraints = GridBagConstraints()
    currentStackLabelConstraints.weightx = 0.5
    currentStackLabelConstraints.anchor = GridBagConstraints.CENTER
    panel.add(currentLabel, currentStackLabelConstraints)
    panel.add(forwardButton)

    val result = panel {
        row {
            cell(panel).onReset {
                val stackSize = errorsModel.errorDetails.flowStacks.stacks.size
                val currentStack = errorsModel.errorDetails.flowStacks.current.plus(1)
                currentLabel.text = "${currentStack}/${stackSize} Flow Stacks"
            }.horizontalAlign(HorizontalAlign.FILL)
        }
    }

    result.isOpaque = false
    return result
}

fun timesPanel(errorsModel: ErrorsModel): DialogPanel {
    val timesPanel = panel {

        row {
            panel {
                row {
                    label("").bind(
                        JLabel::getText, JLabel::setText, PropertyBinding(
                            get = { "<html><span style=\"color:#808080\">Started:</span><br>${prettyTimeOf(errorsModel.errorDetails.delegate?.firstOccurenceTime)}</html>" },
                            set = {})).verticalAlign(VerticalAlign.TOP)
                }
            }
            panel {
                row {
                    label("").bind(
                        JLabel::getText, JLabel::setText, PropertyBinding(
                            get = { "<html><span style=\"color:#808080\">Last:</span><br>${prettyTimeOf(errorsModel.errorDetails.delegate?.lastOccurenceTime)}</html>" },
                            set = {}))
                }
            }
            panel {
                row {
                    label("").bind(
                        JLabel::getText, JLabel::setText, PropertyBinding(
                            get = { "<html><span style=\"color:#808080\">Frequency:</span><br>${errorsModel.errorDetails.delegate?.dayAvg.toString()} /day</html>" },
                            set = {}))
                }
            }
        }

    }

    timesPanel.isOpaque = false
    return timesPanel
}


private fun prettyTimeOf(date: Date?): String {
    val ptNow = PrettyTime()
    return ptNow.format(date)
}


fun buildServicesPanel(servicesPanel: JPanel, errorsModel: ErrorsModel) {

    servicesPanel.removeAll()
    servicesPanel.layout = FlowLayout(FlowLayout.LEFT, 5, 5)
    errorsModel.errorDetails.delegate?.originServices?.forEach(Consumer {
        val service = JLabel(it.serviceName)
        service.background = Color.DARK_GRAY
        service.border = Borders.empty(2)
        service.isOpaque = true
        servicesPanel.add(service)
    })

}


fun getAffectedServicesTooltip(errorsModel: ErrorsModel): String? {
    var tooltipText = ""
    errorsModel.errorDetails.delegate?.originServices?.forEach(Consumer {
        tooltipText = tooltipText.plus(it.serviceName).plus("<br>")
    })
    return asHtml(tooltipText)
}


private fun backButton(project: Project): JPanel {

    val backButton = JButton(Icons.BACK)
    backButton.addActionListener {
        val actionListener: ErrorsActionsService = project.getService(ErrorsActionsService::class.java)
        actionListener.closeErrorDetails()
    }
    backButton.isOpaque = false
    backButton.isContentAreaFilled = false
    backButton.isBorderPainted = false
    backButton.preferredSize = Dimension(48, 48)
    backButton.maximumSize = Dimension(48, 48)
    backButton.size = Dimension(48, 48)
    backButton.rolloverIcon = Icons.BACK_ROLLOVER

    val wrapper = JPanel()
    wrapper.layout = GridBagLayout()
    wrapper.add(backButton)
    wrapper.preferredSize = Dimension(48, 48)
    wrapper.maximumSize = Dimension(48, 48)
    wrapper.isOpaque = false

    return wrapper
}

private fun namePanel(errorsModel: ErrorsModel): DialogPanel {

    val result = panel {
        indent {
            row {
                label("").horizontalAlign(HorizontalAlign.FILL).bind(
                    JLabel::getText, JLabel::setText, PropertyBinding(
                        get = { asHtml(errorsModel.errorDetails.createErrorName()) },
                        set = {})
                )
            }
        }
    }

    result.isOpaque = false
    result.border = Borders.customLine(Color.DARK_GRAY, 0, 2, 0, 0)
    return result
}

private fun scorePanel(errorsModel: ErrorsModel): DialogPanel {
    val result = panel {
        row {
            val scorePanelWrapper = JPanel()
            scorePanelWrapper.isOpaque = false
//            if (errorsModel.errorDetails.delegate != null){
//                var scorePanel = createScorePanel(tempCodeObjectError(errorsModel.errorDetails.delegate!!))
//                scorePanel.isOpaque = false
//                scorePanelWrapper.add(scorePanel)
//            }
            cell(scorePanelWrapper).onReset {
                if (scorePanelWrapper.components.size > 0) {
                    scorePanelWrapper.remove(0)
                }
                if (errorsModel.errorDetails.delegate != null) {
                    //create a dummy CodeObjectErrorDetails just so we can use the same createScorePanel function
                    val scorePanel = createScorePanel(tempCodeObjectError(errorsModel.errorDetails.delegate!!))
                    scorePanel.isOpaque = false
                    scorePanelWrapper.add(scorePanel)
                }
            }.horizontalAlign(HorizontalAlign.FILL).gap(RightGap.SMALL)
        }
    }

    result.isOpaque = false
    return result
}


private fun tempCodeObjectError(errorDetails: CodeObjectErrorDetails): CodeObjectError {
    return CodeObjectError("",
        errorDetails.name,
        errorDetails.scoreInfo,
        errorDetails.sourceCodeObjectId,
        "",
        false,
        false,
        errorDetails.firstOccurenceTime,
        errorDetails.lastOccurenceTime)
}