@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.insights

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.PropertyBinding
import com.intellij.util.Producer
import com.intellij.util.ui.JBEmptyBorder
import com.jetbrains.rd.ui.bedsl.dsl.spacer
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.ui.common.ScopeLineIconProducer
import org.digma.intellij.plugin.ui.common.scopeLine
import org.digma.intellij.plugin.ui.common.topLine
import org.digma.intellij.plugin.ui.env.envCombo
import org.digma.intellij.plugin.ui.model.InsightsModel
import javax.swing.JLabel


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
        row{
            var scopeLine = scopeLine(project,{ insightsModel.classAndMethod() },ScopeLineIconProducer(insightsModel))
            cell(scopeLine)
                .horizontalAlign(HorizontalAlign.FILL)
                .onReset {
                    scopeLine.reset()
                }
        }.layout(RowLayout.PARENT_GRID)

    }


      result.border = JBEmptyBorder(10)

    return result
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
