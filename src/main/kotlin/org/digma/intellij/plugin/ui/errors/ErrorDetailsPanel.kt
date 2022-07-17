package org.digma.intellij.plugin.ui.errors

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.util.ui.JBUI.Borders
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.errordetails.CodeObjectErrorDetails
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.service.ErrorsActionsService
import org.digma.intellij.plugin.ui.common.*
import org.digma.intellij.plugin.ui.list.ScrollablePanelList
import org.digma.intellij.plugin.ui.list.errordetails.ErrorFramesPanelList
import org.digma.intellij.plugin.ui.list.listBackground
import org.digma.intellij.plugin.ui.model.errors.ErrorsModel
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import org.ocpsoft.prettytime.PrettyTime
import java.awt.*
import java.util.*
import java.util.function.Consumer
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants


fun errorDetailsPanel(project: Project, errorsModel: ErrorsModel): DigmaTabPanel {

    val backButton = backButton(project)
    val backButtonWrapper = JPanel()
    backButtonWrapper.layout = BorderLayout()
    backButtonWrapper.isOpaque = false
    backButtonWrapper.border = Borders.empty(5,5,0,0)
    backButtonWrapper.add(backButton,BorderLayout.NORTH)

    val namePanel = namePanel(errorsModel)

    val scorePanel = scorePanel(errorsModel)
    val scorePanelWrapper = JPanel()
    scorePanelWrapper.isOpaque = false
    scorePanelWrapper.layout = GridLayout(1,1)
    scorePanelWrapper.border = Borders.empty(5, 2, 5, 2)
    scorePanelWrapper.add(scorePanel)


    val titlePanel = JPanel()
    titlePanel.isOpaque = false
    titlePanel.layout = BorderLayout(10, 10)
    titlePanel.add(backButtonWrapper,BorderLayout.WEST)
    titlePanel.add(namePanel,BorderLayout.CENTER)
    val titlePanelWrapper = JPanel()
    titlePanelWrapper.layout = GridLayout(1, 1)
    titlePanelWrapper.isOpaque = false
    titlePanelWrapper.border = Borders.empty(0, 5, 0, 0)
    titlePanelWrapper.add(titlePanel)


    val servicesPanel = JPanel()
    servicesPanel.layout = FlowLayout(FlowLayout.LEFT, 0, 5)
    servicesPanel.isOpaque = false
    buildServicesPanel(servicesPanel, errorsModel)
    val affectedServicesLabel = JLabel("Affected Services")
    boldFonts(affectedServicesLabel)
    affectedServicesLabel.toolTipText = getAffectedServicesTooltip(errorsModel)
    val servicesPanelWrapper = JPanel()
    servicesPanelWrapper.layout = BorderLayout()
    servicesPanelWrapper.isOpaque = false
    servicesPanelWrapper.border = Borders.empty(0, 5, 0, 0)
    servicesPanelWrapper.add(affectedServicesLabel, BorderLayout.NORTH)
    servicesPanelWrapper.add(servicesPanel, BorderLayout.CENTER)


    val timesPanel = timesPanel(errorsModel)
    val timesPanelWrapper = JPanel()
    timesPanelWrapper.layout = GridLayout(1, 1)
    timesPanelWrapper.isOpaque = false
    timesPanel.border = Borders.empty(0, 5, 0, 0)
    timesPanelWrapper.add(timesPanel)



    val framesList = ScrollablePanelList(ErrorFramesPanelList(project,
        errorsModel.errorDetails.flowStacks.getCurrentStack(), false))

    val flowStackNavigation = flowStackNavigation(errorsModel,framesList)



    val bottomPanel = bottomPanel(project,errorsModel,framesList)
    bottomPanel.border = Borders.empty(0, 5, 0, 5)

    val result = object : DigmaTabPanel() {
        override fun getPreferredFocusableComponent(): JComponent {
            return backButton.getComponent(0) as JComponent
        }

        override fun getPreferredFocusedComponent(): JComponent {
            return backButton.getComponent(0) as JComponent
        }

        //reset must be called from EDT
        override fun reset() {

            //for intellij DialogPanel instances call reset.
            //for others call inside SwingUtilities.invokeLater

            namePanel.reset()
            scorePanel.reset()
            timesPanel.reset()
            flowStackNavigation.reset()

            buildServicesPanel(servicesPanel, errorsModel)
            affectedServicesLabel.toolTipText = getAffectedServicesTooltip(errorsModel)
            servicesPanel.revalidate()
            servicesPanel.repaint()
            servicesPanelWrapper.revalidate()

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
    constraints.weightx = 0.5
    constraints.weighty = 0.0
    constraints.ipady = 20
    constraints.fill = GridBagConstraints.HORIZONTAL
    result.add(titlePanelWrapper, constraints)

    constraints.gridx = 0
    constraints.gridy = 1
    constraints.weightx = 0.5
    constraints.weighty = weightyForServicesPanel(errorsModel)
    constraints.fill = GridBagConstraints.BOTH
    constraints.ipady = 20
    result.add(servicesPanelWrapper, constraints)


    constraints.gridx = 1
    constraints.gridy = 0
    constraints.weightx = 0.0
    constraints.weighty = 0.0
    constraints.gridheight = 2
    constraints.fill = GridBagConstraints.NONE
    constraints.anchor = GridBagConstraints.NORTH
    constraints.ipady = 10
    constraints.ipadx = 10
    result.add(scorePanelWrapper, constraints)

    constraints.anchor = GridBagConstraints.CENTER
    constraints.gridheight = 1
    constraints.gridwidth = 2
    constraints.gridx = 0
    constraints.gridy = 2
    constraints.weightx = 0.5
    constraints.weighty = 0.0
    constraints.fill = GridBagConstraints.HORIZONTAL
    constraints.ipady = 20
    result.add(timesPanelWrapper, constraints)


    constraints.gridx = 0
    constraints.gridy = 3
    constraints.weightx = 0.5
    constraints.weighty = 0.0
    constraints.fill = GridBagConstraints.HORIZONTAL
    constraints.ipady = 10
    constraints.gridx = 0
    result.add(flowStackNavigation, constraints)

    constraints.gridx = 0
    constraints.gridy = 4
    constraints.weightx = 0.5
    constraints.weighty = 1.0
    constraints.fill = GridBagConstraints.BOTH
    constraints.ipady = 0
    constraints.gridx = 0
    result.add(framesList, constraints)

    constraints.gridx = 0
    constraints.gridy = 5
    constraints.weightx = 0.5
    constraints.weighty = 0.0
    constraints.fill = GridBagConstraints.HORIZONTAL
    result.add(bottomPanel, constraints)

    result.background = listBackground()
    return result

}



fun bottomPanel(project: Project,errorsModel: ErrorsModel, framesList: ScrollablePanelList): JPanel {

    return panel {
        row {
            checkBox("Workspace only")
                .horizontalAlign(HorizontalAlign.LEFT).applyToComponent {
                    isOpaque = false
                    isContentAreaFilled = false
                    isBorderPainted = false
                    isSelected = project.getService(PersistenceService::class.java).state.isWorkspaceOnly
                    addActionListener(){
                        errorsModel.errorDetails.flowStacks.isWorkspaceOnly = isSelected
                        project.getService(PersistenceService::class.java).state.isWorkspaceOnly = isSelected
                        framesList.getModel().setListData(errorsModel.errorDetails.flowStacks.getCurrentStack())
                    }
                }

            link("Open raw trace") {
                val actionListener: ErrorsActionsService = project.getService(ErrorsActionsService::class.java)
                val currentStack = errorsModel.errorDetails.flowStacks.current
                val stackTrace = errorsModel.errorDetails.delegate?.errors?.get(currentStack)?.stackTrace
                actionListener.openRawStackTrace(stackTrace)
            }.horizontalAlign(HorizontalAlign.RIGHT).gap(RightGap.SMALL)


        }.layout(RowLayout.INDEPENDENT).topGap(TopGap.SMALL)
    }.andTransparent()
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

    val currentLabel = JLabel("0/00 Flow Stacks")

//    val size = Laf.scalePanels(Icons.ERROR_DETAILS_NAVIGATION_BUTTON_SIZE)
    val size = Laf.Sizes.ERROR_DETAILS_NAVIGATION_BUTTON_SIZE
    val buttonsSize = Dimension(size + 2, size + 2)

    val backButton = NavigationButtonIcon(Icons.BACK_WHITE, Icons.BACK_BLACK)
    backButton.preferredSize = buttonsSize
    backButton.maximumSize = buttonsSize
    backButton.addActionListener {
        val stackSize = errorsModel.errorDetails.flowStacks.stacks.size
        errorsModel.errorDetails.flowStacks.goBack()
        val currentStack = errorsModel.errorDetails.flowStacks.current.plus(1)
        currentLabel.text = "${currentStack}/${stackSize} Flow Stacks"
        framesList.getModel().setListData(errorsModel.errorDetails.flowStacks.getCurrentStack())
    }


    val forwardButton = NavigationButtonIcon(Icons.FORWARD_WHITE,Icons.FORWARD_BLACK)
    forwardButton.preferredSize = buttonsSize
    forwardButton.maximumSize = buttonsSize
    forwardButton.addActionListener {
        val stackSize = errorsModel.errorDetails.flowStacks.stacks.size
        errorsModel.errorDetails.flowStacks.goForward()
        val currentStack = errorsModel.errorDetails.flowStacks.current.plus(1)
        currentLabel.text = "${currentStack}/${stackSize} Flow Stacks"
        framesList.getModel().setListData(errorsModel.errorDetails.flowStacks.getCurrentStack())
    }


    val panel = JPanel()
    panel.layout = GridBagLayout()

    val backButtonConstraints = GridBagConstraints()
    backButtonConstraints.fill = GridBagConstraints.NONE
    backButtonConstraints.ipadx = 5
    backButtonConstraints.anchor = GridBagConstraints.WEST
    panel.add(backButton, backButtonConstraints)


    val currentStackLabelConstraints = GridBagConstraints()
    currentStackLabelConstraints.fill = GridBagConstraints.NONE
    currentStackLabelConstraints.gridx = 1
    currentStackLabelConstraints.weightx = 0.5
    currentStackLabelConstraints.anchor = GridBagConstraints.CENTER
    panel.add(currentLabel, currentStackLabelConstraints)



    val forwardButtonConstraints = GridBagConstraints()
    forwardButtonConstraints.fill = GridBagConstraints.NONE
    forwardButtonConstraints.gridx = 2
    forwardButtonConstraints.anchor = GridBagConstraints.EAST
    panel.add(forwardButton,forwardButtonConstraints)
    panel.border = Borders.empty(0,1,0,1)

    return panel {
        row {
            cell(panel).onReset {
                val stackSize = errorsModel.errorDetails.flowStacks.stacks.size
                val currentStack = errorsModel.errorDetails.flowStacks.current.plus(1)
                currentLabel.text = "${currentStack}/${stackSize} Flow Stacks"
            }.horizontalAlign(HorizontalAlign.FILL)
        }
    }.andTransparent()

}




fun timesPanel(errorsModel: ErrorsModel): DialogPanel {
    return panel {
        row {
            panel {
                row {
                    label("").bind(
                        JLabel::getText, JLabel::setText, MutableProperty(
                            getter = {
                                buildTimeSpanHtml("Started:",
                                    errorsModel.errorDetails.delegate?.firstOccurenceTime)
                            },
                            setter = {})).verticalAlign(VerticalAlign.TOP)
                }
            }
            panel {
                row {
                    label("").bind(
                        JLabel::getText, JLabel::setText, MutableProperty(
                            getter = {
                                buildTimeSpanHtml("Started:",
                                    errorsModel.errorDetails.delegate?.lastOccurenceTime)
                            },
                            setter = {}))
                }
            }
            panel {
                row {
                    label("").bind(
                        JLabel::getText, JLabel::setText, MutableProperty(
                            getter = { asHtml("${spanGrayed("Frequency:")}<br>${span(errorsModel.errorDetails.delegate?.dayAvg.toString())} /day") },
                            setter = {}))
                }
            }
        }

    }.andTransparent()

}

private fun buildTimeSpanHtml(name: String, value: Date?): String {

    return asHtml("${spanGrayed(name)}<br>${span(prettyTimeOf(value))}")
}


private fun prettyTimeOf(date: Date?): String {
    val ptNow = PrettyTime()
    return ptNow.format(date)
}


fun buildServicesPanel(servicesPanel: JPanel, errorsModel: ErrorsModel) {

    servicesPanel.removeAll()
    servicesPanel.layout = FlowLayout(FlowLayout.LEFT, 0, 5)
    errorsModel.errorDetails.delegate?.originServices?.forEach(Consumer {
        val service = CopyableLabel(it.serviceName)
        service.background = Color.DARK_GRAY
        service.border = Borders.empty(2)
        service.isOpaque = true
        servicesPanel.add(service)
    })

}


fun getAffectedServicesTooltip(errorsModel: ErrorsModel): String {
    var tooltipText = ""
    errorsModel.errorDetails.delegate?.originServices?.forEach(Consumer {
        tooltipText = tooltipText.plus(it.serviceName).plus("<br>")
    })
    return asHtml(tooltipText)
}


private fun backButton(project: Project): JComponent {

    val size = Laf.scalePanels(Laf.Sizes.ERROR_DETAILS_BACK_BUTTON_SIZE)
    val buttonsSize = Dimension(size + 2, size + 3)

    val backButton = BackButton(Icons.BACK_WHITE, Icons.BACK_BLACK)
    backButton.preferredSize = buttonsSize
    backButton.maximumSize = buttonsSize
    backButton.addActionListener {
        val actionListener: ErrorsActionsService = project.getService(ErrorsActionsService::class.java)
        actionListener.closeErrorDetailsBackButton()
    }
    backButton.isOpaque = false
    backButton.isContentAreaFilled = false
    backButton.isBorderPainted = false

    val wrapper = JPanel()
    wrapper.layout = GridLayout(1,1,2,2)
    backButton.horizontalAlignment = SwingConstants.CENTER
    backButton.verticalAlignment = SwingConstants.CENTER
    wrapper.add(backButton)
    wrapper.preferredSize = buttonsSize
    wrapper.maximumSize = buttonsSize
    wrapper.isOpaque = false

    return wrapper
}

private fun namePanel(errorsModel: ErrorsModel): DialogPanel {

    return panel {
        indent {
            row {
                cell(CopyableLabelHtml(""))
                    .horizontalAlign(HorizontalAlign.FILL).bind(
                        CopyableLabelHtml::getText, CopyableLabelHtml::setText, MutableProperty(
                            getter = { buildErrorNameHtml(errorsModel) },
                            setter = {})
                    ).bind(
                        CopyableLabelHtml::getToolTipText, CopyableLabelHtml::setToolTipText, MutableProperty(
                            getter = { buildErrorNameHtml(errorsModel) },
                            setter = {}))

            }
        }
    }.andTransparent().withBorder(Borders.customLine(Color.DARK_GRAY, 0, 2, 0, 0))

}

private fun buildErrorNameHtml(errorsModel: ErrorsModel): String {
    val name = errorsModel.errorDetails.getName()
    val from = errorsModel.errorDetails.getFrom()
    return buildBoldGrayRegularText(name, "From", from)
}




private fun scorePanel(errorsModel: ErrorsModel): DialogPanel {
    return  panel {
        row {
            val scorePanelWrapper = JPanel()
            scorePanelWrapper.isOpaque = false
            cell(scorePanelWrapper).onReset {
                if (scorePanelWrapper.components.size > 0) {
                    scorePanelWrapper.remove(0)
                }
                if (errorsModel.errorDetails.delegate != null) {
                    //create a dummy CodeObjectErrorDetails just so we can use the same createScorePanel function
                    val scorePanel = createScorePanelNoArrows(tempCodeObjectError(errorsModel.errorDetails.delegate!!))
                    scorePanel.isOpaque = false
                    scorePanelWrapper.add(scorePanel)
                }
            }.horizontalAlign(HorizontalAlign.FILL).gap(RightGap.SMALL)
        }
    }.andTransparent()
}


private fun tempCodeObjectError(errorDetails: CodeObjectErrorDetails): CodeObjectError {
    return CodeObjectError("",
        errorDetails.name,
        errorDetails.scoreInfo,
        errorDetails.sourceCodeObjectId,
        "",
        "",
        false,
        false,
        errorDetails.firstOccurenceTime,
        errorDetails.lastOccurenceTime)
}