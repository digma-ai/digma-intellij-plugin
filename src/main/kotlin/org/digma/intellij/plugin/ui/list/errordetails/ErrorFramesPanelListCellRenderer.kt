package org.digma.intellij.plugin.ui.list.errordetails

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.ui.JBUI.Borders
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.service.ErrorsActionsService
import org.digma.intellij.plugin.ui.common.Swing.BLUE_LIGHT_SHADE
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.htmlSpanSmoked
import org.digma.intellij.plugin.ui.common.htmlSpanTitle
import org.digma.intellij.plugin.ui.list.AbstractPanelListCellRenderer
import org.digma.intellij.plugin.ui.model.errors.FrameItem
import org.digma.intellij.plugin.ui.model.errors.FrameListViewItem
import org.digma.intellij.plugin.ui.model.errors.FrameStackTitle
import org.digma.intellij.plugin.ui.model.errors.SpanTitle
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.BorderLayout
import java.awt.Color
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

class ErrorFramesPanelListCellRenderer : AbstractPanelListCellRenderer() {


    @Suppress("UNCHECKED_CAST")
    override fun createPanel(project: Project, value: ListViewItem<*>, index: Int): JPanel {
        return getOrCreatePanel(project,value as ListViewItem<FrameListViewItem>)
    }

    private fun getOrCreatePanel(project: Project, value: ListViewItem<FrameListViewItem>): JPanel {

        val panel =
            when (val modelObject = value.modelObject) {
                is FrameStackTitle -> frameStackTitlePanel(modelObject)
                is SpanTitle -> spanTitlePanel(modelObject)
                is FrameItem -> framePanel(project,modelObject)
                else -> throw RuntimeException("Unknown modelObject $modelObject")
            }

        return panel
    }

    private fun framePanel(project: Project,modelObject: FrameItem): JPanel {
        val panel = panel {
            indent {
                val frameTextPrefix = if(modelObject.frame.modulePhysicalPath.isBlank()) modelObject.frame.moduleName
                        else modelObject.frame.modulePhysicalPath
                val frameText = "${frameTextPrefix} in ${modelObject.frame.functionName}"
                if (modelObject.frame.executedCode.isBlank()){
                    row {
                        if (modelObject.first) {
                            icon(Icons.EVENT_RED).horizontalAlign(HorizontalAlign.LEFT)
                        }
                        if(modelObject.isInWorkspace()){
                            link(frameText) {
                                val actionListener: ErrorsActionsService = project.getService(ErrorsActionsService::class.java)
                                actionListener.openErrorFrameWorkspaceFile(modelObject.getWorkspaceUrl(),modelObject.lastInstanceCommitId,modelObject.frame.lineNumber)
                            }.gap(RightGap.COLUMNS).horizontalAlign(HorizontalAlign.FILL).applyToComponent {
                                toolTipText = frameText
                            }
                        }else {
                            label(frameText)
                                .gap(RightGap.COLUMNS).horizontalAlign(HorizontalAlign.FILL)
                                .applyToComponent {
                                    foreground = Color.GRAY
                                    toolTipText = frameText
                                }
                        }
                        label("line ${modelObject.frame.lineNumber}").horizontalAlign(HorizontalAlign.RIGHT).applyToComponent {
                            foreground = Color.GRAY
                        }
                    }.bottomGap(BottomGap.NONE).topGap(TopGap.NONE)
                }else{
                    //todo: this is relevant for python only where we have executed code, check it when doing pycharm
                    row {
                        label(frameText)
                            .gap(RightGap.COLUMNS).horizontalAlign(HorizontalAlign.FILL).applyToComponent {
                                foreground = Color.GRAY
                                toolTipText = frameText
                            }
                    }
                    row{
                        if (modelObject.first) {
                            icon(Icons.EVENT_RED).horizontalAlign(HorizontalAlign.LEFT)
                        }
                        if(modelObject.isInWorkspace()){
                            link(modelObject.frame.executedCode) {
                                //todo: action
                            }.gap(RightGap.COLUMNS).horizontalAlign(HorizontalAlign.FILL)
                        }else {
                            label(modelObject.frame.executedCode)
                                .gap(RightGap.COLUMNS).horizontalAlign(HorizontalAlign.FILL).applyToComponent {
                                    foreground = Color.GRAY
                                }
                        }
                        label("line ${modelObject.frame.lineNumber}").horizontalAlign(HorizontalAlign.RIGHT).applyToComponent {
                            foreground = Color.GRAY
                        }
                    }
                }
            }
        }

        return itemPanel(panel)
    }

    private fun spanTitlePanel(modelObject: SpanTitle): JPanel {
        val panel = JPanel()
        panel.layout = BorderLayout()
        panel.isOpaque = false

        val icon = JLabel(Icons.TELESCOPE_BLUE_LIGHT_SHADE)
        icon.foreground = BLUE_LIGHT_SHADE
        icon.horizontalAlignment = SwingConstants.LEFT
        icon.border = Borders.empty()
        val iconPanel = JPanel()
        iconPanel.layout = GridBagLayout()
        iconPanel.isOpaque = false
        iconPanel.add(icon)

        val name = JLabel(modelObject.spanName,SwingConstants.LEADING)
        name.horizontalAlignment = SwingConstants.LEFT
        name.horizontalTextPosition = SwingConstants.LEADING
        name.foreground = BLUE_LIGHT_SHADE
        name.border = Borders.empty()
        val namePanel = JPanel()
        namePanel.layout = BorderLayout()
        namePanel.isOpaque = false
        namePanel.border = Borders.empty(0,5,0,0)
        namePanel.add(name,BorderLayout.WEST)



        panel.add(iconPanel,BorderLayout.WEST)
        panel.add(namePanel,BorderLayout.CENTER)
        panel.isOpaque = false
        return itemPanel(panel)
    }


    private fun frameStackTitlePanel(modelObject: FrameStackTitle): JPanel {
        val panel = JPanel()
        panel.layout = GridLayout(1,1)
        val text = "${htmlSpanTitle()}<b>${modelObject.frameStack.exceptionType}<b><br> ${htmlSpanSmoked()}${modelObject.frameStack.exceptionMessage}"
        val label = JLabel(asHtml(text))
        label.toolTipText = "${modelObject.frameStack.exceptionType} ${modelObject.frameStack.exceptionMessage}"
        panel.add(label)
        panel.border = Borders.empty(3)
        panel.isOpaque = false
        return panel
    }



    private fun itemPanel(panel: JPanel): JPanel {
        panel.border = Borders.customLine(Color.GRAY,0,2,0,0)
        panel.isOpaque = false

        val mouseListener = object: MouseAdapter(){
            override fun mouseEntered(e: MouseEvent?) {
                panel.isOpaque = true
                panel.repaint()
            }
            override fun mouseExited(e: MouseEvent?) {
                panel.isOpaque = false
                panel.repaint()
            }
        }

        panel.addMouseListener(mouseListener)
        panel.components.forEach {
            it.addMouseListener(mouseListener)
        }

        return panel
    }



}