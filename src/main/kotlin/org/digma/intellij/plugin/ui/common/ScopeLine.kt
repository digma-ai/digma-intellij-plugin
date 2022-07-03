@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.layout.PropertyBinding
import com.intellij.util.Producer
import javax.swing.Icon
import javax.swing.JLabel


fun scopeLine(project: Project, scopeNameProducer: Producer<String>, scopeIconProducer: Producer<Icon>): DialogPanel {

    return panel {
        row(asHtml(spanGrayed("Scope: "))) {
            icon(scopeIconProducer.produce()).bind(
                JLabel::getIcon, JLabel::setIcon, PropertyBinding(
                    get = { scopeIconProducer.produce() },
                    set = {}))
                .horizontalAlign(HorizontalAlign.RIGHT)
            cell(CopyableLabel("")).bind(
                CopyableLabel::getText, CopyableLabel::setText, PropertyBinding(
                    get = { scopeNameProducer.produce() },
                    set = {})
            ).bind(
                CopyableLabel::getToolTipText, CopyableLabel::setToolTipText, PropertyBinding(
                    get = { scopeNameProducer.produce() },
                    set = {})).horizontalAlign(HorizontalAlign.FILL)
        }.layout(RowLayout.PARENT_GRID)
    }.andTransparent()
}