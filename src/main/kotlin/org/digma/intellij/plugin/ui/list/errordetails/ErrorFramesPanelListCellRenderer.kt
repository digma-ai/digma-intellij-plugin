package org.digma.intellij.plugin.ui.list.errordetails

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.ui.JBUI.Borders
import org.digma.intellij.plugin.icons.Icons
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
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

class ErrorFramesPanelListCellRenderer : AbstractPanelListCellRenderer() {


    @Suppress("UNCHECKED_CAST")
    override fun createPanel(project: Project, value: ListViewItem<*>, index: Int): JPanel {
        return getOrCreatePanel(project,value as ListViewItem<FrameListViewItem>)
    }

    private fun getOrCreatePanel(project: Project, value: ListViewItem<FrameListViewItem>): JPanel {

        val modelObject = value.modelObject

        val panel =
            when (modelObject) {
                is FrameStackTitle -> frameStackTitlePanel(modelObject)
                is SpanTitle -> spanTitlePanel(modelObject)
                is FrameItem -> framePanel(modelObject)
                else -> throw RuntimeException("Unknown modelObject $modelObject")
            }

        return panel
    }

    private fun framePanel(modelObject: FrameItem): JPanel {
        val panel = panel {
            indent {
                if (modelObject.frame.executedCode.isBlank()){
                    row {
                        if (modelObject.first) {
                            icon(Icons.RED_THUNDER).horizontalAlign(HorizontalAlign.LEFT)
                        }
                        if(modelObject.workspaceUrl?.isBlank()!!){
                            label("${modelObject.frame.moduleName} in ${modelObject.frame.functionName}")
                                .gap(RightGap.COLUMNS).horizontalAlign(HorizontalAlign.FILL).applyToComponent {
                                    foreground = Color.GRAY
                                }
                        }else {
                            link("${modelObject.frame.moduleName} in ${modelObject.frame.functionName}") {

                            }.gap(RightGap.COLUMNS).horizontalAlign(HorizontalAlign.FILL)
                        }
                        label("line ${modelObject.frame.lineNumber}").horizontalAlign(HorizontalAlign.RIGHT).applyToComponent {
                            foreground = Color.GRAY
                        }
                    }.bottomGap(BottomGap.NONE).topGap(TopGap.NONE)
                }else{
                    //todo else
                }
            }
        }

        return itemPanel(panel)
    }

    private fun spanTitlePanel(modelObject: SpanTitle): JPanel {
        val panel = JPanel()
        panel.layout = BorderLayout()
        panel.isOpaque = false

        val icon = JLabel(Icons.TELESCOPE_32)
        icon.horizontalAlignment = SwingConstants.LEFT
        icon.border = Borders.empty()
        val iconPanel = JPanel()
        iconPanel.layout = GridBagLayout()
        iconPanel.isOpaque = false
        iconPanel.add(icon)

        val name = JLabel(modelObject.spanName,SwingConstants.LEADING)
        name.horizontalAlignment = SwingConstants.LEFT
        name.horizontalTextPosition = SwingConstants.LEADING
        name.foreground = Color(125,160,220)
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
        val text = "<html>${modelObject.frameStack.exceptionType}<br> <span style=\"color:#808080\">${modelObject.frameStack.exceptionMessage}</span></html>"
        val label = JLabel(text)
        panel.add(label)
        panel.isOpaque = false
        return panel
    }



    private fun itemPanel(panel: JPanel): JPanel {
        panel.border = Borders.customLine(Color.GRAY,0,2,0,0)
        panel.isOpaque = false
        return panel
    }



}