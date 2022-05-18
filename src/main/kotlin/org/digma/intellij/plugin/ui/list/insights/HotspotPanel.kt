package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.util.ui.JBUI.Borders
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.HotspotInsight
import org.digma.intellij.plugin.ui.common.iconPanel
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import javax.swing.JPanel


fun hotspotPanel(listViewItem: ListViewItem<HotspotInsight>): JPanel {

    val hotspotInsight: HotspotInsight = listViewItem.getModel()


    val result = panel {
        row("This is an error hotspot") {}
        row {
            panel {
                row {
//                    cell(multiLineLabel(listViewItem.content))
//                    label(listViewItem.content)
                    textArea().bindText({ "Many major errors occur or propagate through this function." }, {})
                        .horizontalAlign(HorizontalAlign.CENTER)
                        .verticalAlign(VerticalAlign.FILL)
                        .resizableColumn()
                        .applyToComponent {
                            this.isEditable = false
                            this.border = Borders.empty()
                            this.isOpaque = true
//                            this.alignmentX = Component.RIGHT_ALIGNMENT
                        }
                }.layout(RowLayout.INDEPENDENT)
                row {
                    link("See how this was calculated", action = {
                        println("in action " + it)
                    })
                }.layout(RowLayout.INDEPENDENT)
                row {
                    browserLink("See how this was calculated", "https://phmecloud.blob.core.windows.net/photo/web/ou0ehpjndrfhkkx1tekojx0-3.png")
                }.layout(RowLayout.INDEPENDENT)
            }.horizontalAlign(HorizontalAlign.FILL)
                .verticalAlign(VerticalAlign.FILL)
            panel{
                row{
                    cell(iconPanel(Icons.HOTSPOT,"HotSpot"))
//                        .horizontalAlign(HorizontalAlign.RIGHT)
                          .verticalAlign(VerticalAlign.FILL)
                }.layout(RowLayout.INDEPENDENT)
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


        }
    }

    result.isOpaque = true
    return result
}