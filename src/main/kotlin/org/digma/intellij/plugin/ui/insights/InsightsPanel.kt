@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.insights

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.ui.JBEmptyBorder
import com.jetbrains.rd.ui.bedsl.dsl.spacer
import org.digma.intellij.plugin.ui.env.envCombo
import org.digma.intellij.plugin.ui.model.InsightsModel


val insightsModel: InsightsModel = InsightsModel()


fun insightsPanel(project: Project): DialogPanel {


    val result = panel {

        row {
            intTextField().bindIntText(insightsModel::insightsCount)
                .horizontalAlign(HorizontalAlign.LEFT)
                .columns(4)
                .bold()
                .gap(RightGap.SMALL)
                .apply {
                    component.isEditable = false
                }
            label("Insights")
                .horizontalAlign(HorizontalAlign.LEFT)
                .bold()
                .gap(RightGap.SMALL)
            spacer()
            cell(envCombo(project))
                .horizontalAlign(HorizontalAlign.RIGHT)
        }.layout(RowLayout.PARENT_GRID)
        panel {
            row("Scope: ") {
                textField().bindText(insightsModel::methodName)
                    .horizontalAlign(HorizontalAlign.FILL)
            }.layout(RowLayout.PARENT_GRID)
        }
    }

    result.border = JBEmptyBorder(10)

    return result
}

