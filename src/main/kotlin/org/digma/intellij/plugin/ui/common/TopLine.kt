@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.digma.intellij.plugin.ui.env.envCombo
import org.digma.intellij.plugin.ui.model.PanelModel
import javax.swing.JLabel


fun topLine(model: PanelModel, labelText: String): DialogPanel {

    return panel {
        row {
            label(model.count()).bind(
                JLabel::getText, JLabel::setText, MutableProperty(
                    getter = { model.count() },
                    setter = {})
            )
                .horizontalAlign(HorizontalAlign.LEFT)
                .bold()
                .gap(RightGap.SMALL)
            label(labelText)
                .horizontalAlign(HorizontalAlign.LEFT)
                .bold()
                .gap(RightGap.SMALL)
            cell(envCombo())
                .horizontalAlign(HorizontalAlign.RIGHT)
        }.layout(RowLayout.INDEPENDENT)
    }.andTransparent()
}