@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.insights

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.util.ui.JBEmptyBorder
import org.digma.intellij.plugin.ui.common.ScopeLineIconProducer
import org.digma.intellij.plugin.ui.common.scopeLine
import org.digma.intellij.plugin.ui.common.topLine
import org.digma.intellij.plugin.ui.list.insights.InsightsList
import org.digma.intellij.plugin.ui.model.insights.InsightsModel
import java.awt.GridBagConstraints
import java.awt.GridBagLayout


val insightsModel: InsightsModel = InsightsModel()

fun insightsPanel(project: Project): DialogPanel {


    val result = panel {
        row {
            var topLine = topLine(project, insightsModel,"Code insights")
            cell(topLine)
                .horizontalAlign(HorizontalAlign.FILL)
                .onReset {
                    topLine.reset()
                }
        }.layout(RowLayout.PARENT_GRID)
        row {
            var scopeLine = scopeLine(project, { insightsModel.classAndMethod() }, ScopeLineIconProducer(insightsModel))
            cell(scopeLine)
                .horizontalAlign(HorizontalAlign.FILL)
                .onReset {
                    scopeLine.reset()
                }
        }.layout(RowLayout.PARENT_GRID)
        row{

            var insightsList = InsightsList()
            insightsList.getModel().setListData(insightsModel.listViewItems)

//            scrollCell(insightsList)

//            val wrapper: JPanel = JPanel()
//            wrapper.layout = BoxLayout(wrapper,BoxLayout.PAGE_AXIS)
//            var hotspotPanel = hotspotPanel(HotspotListViewItem("header","content","link text","http://localhost",0))
//            wrapper.add(hotspotPanel)

//            val c = cell(insightsList)
            val scrollPane = scrollCell(insightsList)
            scrollPane
                .horizontalAlign(HorizontalAlign.FILL)
                .verticalAlign(VerticalAlign.BOTTOM)
                .onReset {
                    insightsList.getModel().setListData(insightsModel.listViewItems)
//                    scrollPane.component.revalidate()
//                    scrollPane.component.repaint()
//                    scrollPane.component.preferredSize = Dimension(-1,400)
                }
        }.layout(RowLayout.PARENT_GRID)
//            .resizableRow()


//        row{
//            label()
//        }
//        row {
//            var insightsListPanel = insightsListPanel(project)
////            buildListPanel(insightsListPanel)
////            insightsListPanel.border = JBEmptyBorder(10)
//            cell(insightsListPanel)
//                .bind((
//                        insightsListPanel(project), insightsListPanel(project), PropertyBinding(
//                    get = { insightsListPanel(project) },
//                    set = {}))
//                .onReset {
//                    buildListPanel(insightsListPanel)
//                }
//        }.resizableRow()
//        row{
//            scrollCell
//            var insightsListPanel = insightsListPanel(project)
//            var result = cell(insightsListPanel)
//                .horizontalAlign(HorizontalAlign.FILL)
//                .onReset {
//
//                }
//        }

    }


      result.border = JBEmptyBorder(10)

    return result
}

fun insightsListPanel(project: Project): DialogPanel {

    return panel {

    }

}

fun buildListPanel(panel: DialogPanel) {
    panel.removeAll()
    var listPanel = panel {
        row {
            label("first row")
        }
        row {
            label("second row")
        }
        row {
            label("third row")
        }
    }

    panel.layout = GridBagLayout()
    panel.add(JBScrollPane(listPanel), GridBagConstraints.CENTER)

    println(panel)
}





//class IsMethodScope: ComponentPredicate(){
//    override fun addListener(listener: (Boolean) -> Unit) {
//        listener.invoke(insightsModel.methodName.isNotBlank())
//    }
//
//    override fun invoke(): Boolean {
//        return insightsModel.methodName.isNotBlank()
//    }
//
//}
