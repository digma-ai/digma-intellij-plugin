package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.util.ui.JBUI.Borders
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.ui.common.iconPanel
import org.digma.intellij.plugin.ui.model.insights.HotspotListViewItem
import javax.swing.JPanel


fun hotspotPanel(listViewItem: HotspotListViewItem): JPanel {

    val result = panel {
        row(listViewItem.header) {}
        row {
            panel {
                row {
//                    cell(multiLineLabel(listViewItem.content))
//                    label(listViewItem.content)
                    textArea().bindText({ listViewItem.content }, {})
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
                    link(listViewItem.linkText, action = {
                        println("in action " + it)
                    })
                }.layout(RowLayout.INDEPENDENT)
                row {
                    browserLink(listViewItem.linkText, listViewItem.linkUrl)
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