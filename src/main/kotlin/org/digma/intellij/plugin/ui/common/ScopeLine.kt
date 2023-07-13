@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBLabel
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
        row {

            //todo: CopyableLabel does work wrap, that's not good here.
            // JBLabel can be copyable but then  there is no tooltip.
            // JLabel is not copyable but there is tooltip.

            icon(scopeIconProducer.produce()).bind(
                JLabel::getIcon, JLabel::setIcon, MutableProperty(
                    getter = { scopeIconProducer.produce() },
                    setter = {}))
                .horizontalAlign(HorizontalAlign.RIGHT)
            cell(JBLabel("")).bind(
                JLabel::getText, JLabel::setText, MutableProperty(
                    getter = { scopeNameProducer.produce() },
                    setter = {})
            ).bind(
                JLabel::getToolTipText, JLabel::setToolTipText, MutableProperty(
                    getter = { scopeTooltipProducer.produce() },
                    setter = {})).horizontalAlign(HorizontalAlign.FILL)
//            cell(CopyableLabel("")).bind(
//                CopyableLabel::getText, CopyableLabel::setText, MutableProperty(
//                    getter = { scopeNameProducer.produce() },
//                    setter = {})
//            ).bind(
//                CopyableLabel::getToolTipText, CopyableLabel::setToolTipText, MutableProperty(
//                    getter = { scopeTooltipProducer.produce() },
//                    setter = {})).horizontalAlign(HorizontalAlign.FILL)
        }.layout(RowLayout.PARENT_GRID)
    }.andTransparent()
}