package org.digma.intellij.plugin.ui.common

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult
import org.digma.intellij.plugin.ui.model.NOT_SUPPORTED_OBJECT_MSG
import org.digma.intellij.plugin.ui.model.PanelModel
import org.digma.intellij.plugin.ui.model.insights.InsightsModel
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.awt.BorderLayout
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JLabel


fun noCodeObjectWarningPanel(model: PanelModel): DialogPanel {

    return panel {
        row {
            icon(AllIcons.General.BalloonInformation)
                .horizontalAlign(HorizontalAlign.CENTER)
        }.bottomGap(BottomGap.MEDIUM).topGap(TopGap.MEDIUM)
        row {
            label(getNoInfoMessage(model)).bind(
                JLabel::getText, JLabel::setText, MutableProperty(
                    getter = { getNoInfoMessage(model) },
                    setter = {})
            ).bind(
                JLabel::getToolTipText, JLabel::setToolTipText, MutableProperty(
                    getter = { getNoInfoMessage(model) },
                    setter = {})
            )
                .horizontalAlign(HorizontalAlign.CENTER)
        }.bottomGap(BottomGap.MEDIUM).topGap(TopGap.MEDIUM)
    }.andTransparent().withBorder(JBUI.Borders.empty())
}


private fun getNoInfoMessage(model: PanelModel): String {
    var msg = if (model is InsightsModel) "No insights" else "No errors"

    if (model.getScope().isNotBlank() && !model.getScope().contains(NOT_SUPPORTED_OBJECT_MSG)) {
        msg += " for " + model.getScope()
    }
    return msg
}


fun createTopPanel(
    project: Project,
    model: PanelModel,
    usageStatusResultRef: AtomicReference<UsageStatusResult>
): DigmaResettablePanel {

    val scopeLine = scopeLine({ model.getScope() }, { model.getScopeTooltip() }, ScopeLineIconProducer(model))
    scopeLine.isOpaque = false

    val envsPanel =
        environmentsPanel(project, AnalyticsService.getInstance(project).environment, usageStatusResultRef)
    envsPanel.isOpaque = false

    val result = object : DigmaResettablePanel() {

        override fun reset() {
            scopeLine.reset()
        }
    }

    result.isOpaque = false
    result.border = JBUI.Borders.empty(0, 10, 0, 10)
    result.layout = BorderLayout()
    result.add(scopeLine, BorderLayout.NORTH)
    result.add(envsPanel, BorderLayout.CENTER)
    return result
}