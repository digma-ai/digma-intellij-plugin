@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.Producer
import javax.swing.Icon
import javax.swing.JLabel


fun scopeLine(scopeNameProducer: Producer<String>,
              scopeTooltipProducer: Producer<String>,
              scopeIconProducer: Producer<Icon>): DialogPanel {

    return panel {
        row(asHtml(spanGrayed("Scope: "))) {
            icon(scopeIconProducer.produce()).bind(
                JLabel::getIcon, JLabel::setIcon, MutableProperty(
                    getter = { scopeIconProducer.produce() },
                    setter = {}))
                .horizontalAlign(HorizontalAlign.RIGHT)
            cell(CopyableLabel("")).bind(
                CopyableLabel::getText, CopyableLabel::setText, MutableProperty(
                    getter = { scopeNameProducer.produce() },
                    setter = {})
            ).bind(
                CopyableLabel::getToolTipText, CopyableLabel::setToolTipText, MutableProperty(
                    getter = { scopeTooltipProducer.produce() },
                    setter = {})).horizontalAlign(HorizontalAlign.FILL)
        }.layout(RowLayout.PARENT_GRID)
    }.andTransparent()
}