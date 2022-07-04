@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.layout.PropertyBinding
import com.jetbrains.rd.ui.bedsl.dsl.spacer
import org.digma.intellij.plugin.ui.env.envCombo
import org.digma.intellij.plugin.ui.model.PanelModel
import javax.swing.JLabel


fun topLine(project: Project, model: PanelModel, labelText: String): DialogPanel {

    return panel {
        row {
            label(model.count()).bind(
                JLabel::getText, JLabel::setText, PropertyBinding(
                    get = { model.count() },
                    set = {})
            )
                .horizontalAlign(HorizontalAlign.LEFT)
                .bold()
                .gap(RightGap.SMALL)
            spacer()
            label(labelText)
                .horizontalAlign(HorizontalAlign.LEFT)
                .bold()
                .gap(RightGap.SMALL)
            cell(envCombo(project))
                .horizontalAlign(HorizontalAlign.RIGHT)
        }.layout(RowLayout.PARENT_GRID)
    }.andTransparent()
}