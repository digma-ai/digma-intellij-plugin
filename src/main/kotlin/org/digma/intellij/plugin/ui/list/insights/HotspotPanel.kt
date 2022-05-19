package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.RowLayout.INDEPENDENT
import com.intellij.ui.dsl.builder.RowLayout.PARENT_GRID
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.HotspotInsight
import org.digma.intellij.plugin.ui.common.iconPanel
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JLabel
import javax.swing.JPanel


fun hotspotPanel(listViewItem: ListViewItem<HotspotInsight>): JPanel {

    val hotspotInsight: HotspotInsight = listViewItem.modelObject


    val result = panel {
        row("This is an error hotspot") {}
        row {
            panel {
                row {

//                    var text = "Many major errors occur or propagate through this function. Many major errors occur or propagate through this function."
//                    var html = "<html>%1s</html>"
//                    var html = "<html><body style='width: %1spx'>%1s</body></html>"

//                    val label = JLabel(String.format(html, 500, text))
//                    val label = JLabel(String.format(html, text))
//                    val panel = JPanel(BorderLayout())
//                    panel.add(label, BorderLayout.NORTH)
//                    panel.preferredSize = Dimension(640, 480)
//                    cell(panel)
//                    cell(multiLineLabel(listViewItem.content))
                    label("<html>Many major errors occur or propagate through this function.Many major errors occur or propagate through this function.")
                        .horizontalAlign(HorizontalAlign.FILL)
                        .applyToComponent {
//                            toolTipText = "Many major errors occur or propagate through this function."
//                            maximumSize = Dimension(300,0)
//                            preferredSize = Dimension(300,30)
                        }
//                    textArea().bindText({ "Many major errors occur or propagate through this function." }, {})
//                        .horizontalAlign(HorizontalAlign.CENTER)
//                        .verticalAlign(VerticalAlign.FILL)
//                        .resizableColumn()
//                        .applyToComponent {
//                            this.isEditable = false
//                            this.border = Borders.empty()
//                            this.isOpaque = true
////                            this.alignmentX = Component.RIGHT_ALIGNMENT
//                        }
                }.layout(PARENT_GRID)
//                row {
//                    link("See how this was calculated", action = {
//                        println("in action " + it)
//                    })
//                }//.layout(RowLayout.INDEPENDENT)
//                row {
//                    browserLink("See how this was calculated", "https://phmecloud.blob.core.windows.net/photo/web/ou0ehpjndrfhkkx1tekojx0-3.png")
//                }//.layout(RowLayout.INDEPENDENT)
            }.horizontalAlign(HorizontalAlign.LEFT)
                .verticalAlign(VerticalAlign.FILL)
            panel{
                row{
                    cell(iconPanel(Icons.HOTSPOT,"HotSpot"))
                        .horizontalAlign(HorizontalAlign.RIGHT)
                          .verticalAlign(VerticalAlign.FILL)
                }.layout(PARENT_GRID)
            }.horizontalAlign(HorizontalAlign.RIGHT)
                .verticalAlign(VerticalAlign.FILL)
//            panel {
//                row {
////                    val icon = Icons.HOTSPOT_PANEL.
////                    var icon = IconLoader.getIcon("/icons/hotspot.png", Icon::class.java)
//
////                    val ii = ImageIcon(ImageIcon("icons/hotspot.png").image.getScaledInstance(30,30,
////                        SCALE_SMOOTH))
//                    icon(AllIcons.General.Error)
//                        .horizontalAlign(HorizontalAlign.LEFT)
//                        .verticalAlign(VerticalAlign.CENTER)
//                }.layout(RowLayout.INDEPENDENT).bottomGap(BottomGap.MEDIUM).topGap(TopGap.MEDIUM)
//                row {
//                    label("HotSpot")
//                        .horizontalAlign(HorizontalAlign.RIGHT)
//                        .verticalAlign(VerticalAlign.BOTTOM)
////                        .gap(RightGap.SMALL)
//                }.layout(RowLayout.INDEPENDENT)
//
//            }.horizontalAlign(HorizontalAlign.RIGHT)
//                .verticalAlign(VerticalAlign.CENTER)


        }.layout(PARENT_GRID)
    }

    result.isOpaque = true
    return result
}