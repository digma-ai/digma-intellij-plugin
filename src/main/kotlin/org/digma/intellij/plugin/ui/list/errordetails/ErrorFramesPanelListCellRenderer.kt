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
import org.digma.intellij.plugin.ui.common.*
import org.digma.intellij.plugin.ui.list.AbstractPanelListCellRenderer
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import org.digma.intellij.plugin.ui.model.errors.FrameItem
import org.digma.intellij.plugin.ui.model.errors.FrameListViewItem
import org.digma.intellij.plugin.ui.model.errors.FrameStackTitle
import org.digma.intellij.plugin.ui.model.errors.SpanTitle
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants


class ErrorFramesPanelListCellRenderer : AbstractPanelListCellRenderer() {


    @Suppress("UNCHECKED_CAST")
    override fun createPanel(project: Project,
                             value: ListViewItem<*>,
                             index: Int,
                             panelsLayoutHelper: PanelsLayoutHelper): JPanel {
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
                val frameText = "$frameTextPrefix in ${modelObject.frame.functionName}"
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
                                toolTipText = "$frameText line ${modelObject.frame.lineNumber}"
                            }
                        }else {
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
                }else{
                    //todo: this is relevant for python only where we have executed code, check it when doing pycharm
                    row {
                        cell(CopyableLabel(frameText))
                            .gap(RightGap.COLUMNS).horizontalAlign(HorizontalAlign.FILL).applyToComponent {
                                toolTipText = "$frameText line ${modelObject.frame.lineNumber}"
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

        val icon = JLabel(Icons.TELESCOPE_BLUE_LIGHT_SHADE)
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
        namePanel.border = Borders.empty(0,Laf.scaleBorders(5),0,0)
        namePanel.add(name,BorderLayout.WEST)

        val result = JPanel()
        result.layout = BorderLayout()
        result.isOpaque = false
        result.add(iconPanel,BorderLayout.WEST)
        result.add(namePanel,BorderLayout.CENTER)
        return itemPanel(result)
    }


    private fun frameStackTitlePanel(modelObject: FrameStackTitle): JPanel {
        val panel = JPanel()
        panel.layout = GridLayout(1,1)
        val text = buildTitleItalicGrayedComment(modelObject.frameStack.exceptionType,modelObject.frameStack.exceptionMessage)
        val label = CopyableLabelHtml(text)
        label.toolTipText = text
        panel.add(label)
        panel.border = Borders.empty(Laf.scaleBorders(3),Laf.scaleBorders(3),Laf.scaleBorders(5),Laf.scaleBorders(3))
        panel.isOpaque = false
        return panel
    }



    private fun itemPanel(panel: JPanel): JPanel {
        panel.isOpaque = false

        val wrapper = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                g.color = background
                g.fillRect(0, 0, width, height)
                super.paintComponent(g)
            }
        }
        wrapper.layout = GridLayout(1,1)
        wrapper.add(panel)
        wrapper.border = Borders.customLine(Laf.getLabelGrayedColor(),0,2,0,0)
        wrapper.isOpaque = false
        wrapper.background = Laf.Colors.TRANSPARENT

        val mouseListener = object: MouseAdapter(){
            override fun mouseEntered(e: MouseEvent?) {
                wrapper.background = Laf.Colors.LIST_ITEM_BACKGROUND
            }
            override fun mouseExited(e: MouseEvent?) {
                wrapper.background = Laf.Colors.TRANSPARENT
            }
        }

        wrapper.addMouseListener(mouseListener)
        panel.addMouseListener(mouseListener)
        panel.components.forEach {
            it.addMouseListener(mouseListener)
        }

        return wrapper
    }

    class Hover constructor(val component: JComponent, val hoverColor: Color): JPanel() {
        init {
            component.isOpaque = false
            background = component.background
            val mouseListener =object: MouseAdapter(){
                override fun mouseEntered(e: MouseEvent?) {
                    background = hoverColor
                }
                override fun mouseExited(e: MouseEvent?) {
                    background = component.background
                }
            }

            this.addMouseListener(mouseListener)
            component.addMouseListener(mouseListener)
            component.components.forEach {
                it.addMouseListener(mouseListener)
            }
        }

        override fun paintComponent(g: Graphics) {
            g.color = background
            g.fillRect(0, 0, width, height)
            super.paintComponent(g)
        }
    }

}