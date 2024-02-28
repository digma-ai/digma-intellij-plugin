package org.digma.intellij.plugin.ui.list.errordetails

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.ui.JBUI.Borders
import org.digma.intellij.plugin.service.ErrorsActionsService
import org.digma.intellij.plugin.ui.common.CopyableLabel
import org.digma.intellij.plugin.ui.common.CopyableLabelHtml
import org.digma.intellij.plugin.ui.common.Hover
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.TraceButton
import org.digma.intellij.plugin.ui.common.buildTitleItalicGrayedComment
import org.digma.intellij.plugin.ui.list.AbstractPanelListCellRenderer
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import org.digma.intellij.plugin.ui.model.errors.FrameItem
import org.digma.intellij.plugin.ui.model.errors.FrameListViewItem
import org.digma.intellij.plugin.ui.model.errors.FrameStackTitle
import org.digma.intellij.plugin.ui.model.errors.SpanTitle
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.BorderLayout
import java.awt.GridBagLayout
import java.awt.GridLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants


class ErrorFramesPanelListCellRenderer : AbstractPanelListCellRenderer() {


    @Suppress("UNCHECKED_CAST")
    override fun createPanel(
        project: Project,
        value: ListViewItem<*>,
        index: Int,
        panelsLayoutHelper: PanelsLayoutHelper,
    ): JPanel {
        return getOrCreatePanel(project, value as ListViewItem<FrameListViewItem>)
    }

    private fun getOrCreatePanel(project: Project, value: ListViewItem<FrameListViewItem>): JPanel {

        val panel =
            when (val modelObject = value.modelObject) {
                is FrameStackTitle -> frameStackTitlePanel(project, modelObject)
                is SpanTitle -> spanTitlePanel(modelObject)
                is FrameItem -> framePanel(project, modelObject)
                else -> throw RuntimeException("Unknown modelObject $modelObject")
            }

        return panel
    }

    private fun framePanel(project: Project, modelObject: FrameItem): JPanel {
        val panel = panel {
            indent {
                val frameTextPrefix =
                    if (modelObject.frame.modulePhysicalPath.isBlank()) modelObject.frame.moduleName
                    else modelObject.frame.modulePhysicalPath
                val frameText = "$frameTextPrefix in ${modelObject.frame.functionName}"
                if (modelObject.frame.executedCode.isBlank()) {
                    row {
                        if (modelObject.first) {
                            icon(Laf.Icons.ErrorDetails.EVENT_RED).horizontalAlign(HorizontalAlign.LEFT)
                        }
                        if (modelObject.isInWorkspace()) {
                            link(frameText) {
                                val actionListener: ErrorsActionsService = project.getService(ErrorsActionsService::class.java)
                                actionListener.openErrorFrameWorkspaceFile(
                                    modelObject.getWorkspaceUrl(),
                                    modelObject.lastInstanceCommitId,
                                    modelObject.frame.lineNumber
                                )
                            }.gap(RightGap.COLUMNS).horizontalAlign(HorizontalAlign.FILL).applyToComponent {
                                toolTipText = "$frameText line ${modelObject.frame.lineNumber}"
                            }
                        } else {
                            cell(CopyableLabel(frameText))
                                .applyToComponent {
                                    foreground = Laf.getLabelGrayedColor()
                                    toolTipText = "$frameText line ${modelObject.frame.lineNumber}"
                                }.gap(RightGap.COLUMNS).horizontalAlign(HorizontalAlign.FILL)
                        }
                        label("line ${modelObject.frame.lineNumber}").horizontalAlign(HorizontalAlign.RIGHT)
                            .gap(RightGap.SMALL)
                            .applyToComponent {
                                foreground = Laf.getLabelGrayedColor()
                            }
                    }.bottomGap(BottomGap.NONE).topGap(TopGap.NONE)
                } else {
                    //todo: this is relevant for python only where we have executed code, check it when doing pycharm
                    row {
                        cell(CopyableLabel(frameText))
                            .gap(RightGap.COLUMNS).horizontalAlign(HorizontalAlign.FILL).applyToComponent {
                                toolTipText = "$frameText line ${modelObject.frame.lineNumber}"
                            }
                    }
                    row {
                        if (modelObject.first) {
                            icon(Laf.Icons.ErrorDetails.EVENT_RED).horizontalAlign(HorizontalAlign.LEFT)
                        }
                        if (modelObject.isInWorkspace()) {
                            link(modelObject.frame.executedCode) {
                                val actionListener: ErrorsActionsService = project.getService(ErrorsActionsService::class.java)
                                actionListener.openErrorFrameWorkspaceFile(
                                    modelObject.getWorkspaceUrl(),
                                    modelObject.lastInstanceCommitId,
                                    modelObject.frame.lineNumber
                                )
                            }.gap(RightGap.COLUMNS).horizontalAlign(HorizontalAlign.FILL)
                        } else {
                            cell(CopyableLabel(modelObject.frame.executedCode))
                                .gap(RightGap.COLUMNS).horizontalAlign(HorizontalAlign.FILL).applyToComponent {
                                    foreground = Laf.getLabelGrayedColor()
                                }
                        }
                        label("line ${modelObject.frame.lineNumber}").horizontalAlign(HorizontalAlign.RIGHT).applyToComponent {
                            foreground = Laf.getLabelGrayedColor()
                        }
                    }
                }
            }
        }

        return itemPanel(panel)
    }

    private fun spanTitlePanel(modelObject: SpanTitle): JPanel {

        val icon = JLabel(Laf.Icons.ErrorDetails.TELESCOPE_BLUE_LIGHT_SHADE)
        icon.foreground = Laf.Colors.BLUE_LIGHT_SHADE
        icon.horizontalAlignment = SwingConstants.LEFT
        icon.border = Borders.empty()
        val iconPanel = JPanel()
        iconPanel.layout = GridBagLayout()
        iconPanel.isOpaque = false
        iconPanel.border = Borders.empty(2)
        iconPanel.add(icon)

        val name = CopyableLabel(modelObject.spanName)
        name.alignmentX = 0.0f
        name.foreground = Laf.Colors.BLUE_LIGHT_SHADE
        val namePanel = JPanel()
        namePanel.layout = BorderLayout()
        namePanel.isOpaque = false
        namePanel.border = Borders.emptyLeft(5)
        namePanel.add(name, BorderLayout.WEST)

        val result = JPanel()
        result.layout = BorderLayout()
        result.isOpaque = false
        result.add(iconPanel, BorderLayout.WEST)
        result.add(namePanel, BorderLayout.CENTER)
        return itemPanel(result)
    }

    private fun innerFrameStackTitlePanel(modelObject: FrameStackTitle): JPanel {
        val panel = JPanel()
        panel.layout = GridLayout(1, 1)
        val text = buildTitleItalicGrayedComment(modelObject.frameStack.exceptionType, modelObject.frameStack.exceptionMessage)
        val label = CopyableLabelHtml(text)
        label.toolTipText = text
        panel.add(label)
        panel.border = Borders.empty(3, 3, 5, 3)
        panel.isOpaque = false
        return panel
    }

    private fun frameStackTitlePanel(project: Project, modelObject: FrameStackTitle): JPanel {
        val leftPanel = innerFrameStackTitlePanel(modelObject)

        val buttonsPanel = JBPanel<JBPanel<*>>()
        buttonsPanel.layout = BoxLayout(buttonsPanel, BoxLayout.Y_AXIS)
        buttonsPanel.isOpaque = false
        buttonsPanel.border = Borders.emptyRight(5)

        if (modelObject.traceId != null && "NA" != modelObject.traceId) {
            val title = "Sample trace for error ${modelObject.frameStack.exceptionType}"
            val traceButton = TraceButton()
            traceButton.defineAction(project, modelObject.traceId!!, title)
            buttonsPanel.add(traceButton)
            buttonsPanel.add(Box.createVerticalStrut(10))
        }

        val result = JPanel()
        result.layout = BorderLayout(0, 3)
        result.isOpaque = false
        result.add(leftPanel, BorderLayout.CENTER)
        result.add(buttonsPanel, BorderLayout.EAST)
        return result
    }

    private fun itemPanel(panel: JPanel): JPanel {
        panel.border = Borders.customLine(Laf.getLabelGrayedColor(), 0, 2, 0, 0)
        panel.background = Laf.Colors.TRANSPARENT

        return Hover(panel, Laf.Colors.LIST_ITEM_BACKGROUND)
    }
}