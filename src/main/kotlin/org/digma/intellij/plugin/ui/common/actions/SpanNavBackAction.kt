package org.digma.intellij.plugin.ui.common.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.ui.JBColor
import org.digma.intellij.plugin.insights.InsightsViewOrchestrator
import org.digma.intellij.plugin.model.nav.ScopeType
import org.digma.intellij.plugin.navigation.HistoryScopeNavigation
import org.digma.intellij.plugin.navigation.HomeSwitcherService
import org.digma.intellij.plugin.ui.common.Laf

class SpanNavBackAction : AnAction("Go to previous span") {

    companion object {
        const val ID = "DigmaSpanNavBackAction"
    }

    init {
        templatePresentation.icon = if (JBColor.isBright()) {
            Laf.Icons.Common.NavPrevLight
        } else {
            Laf.Icons.Common.NavPrevDark
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        if (project == null) return

        val insightViewOrch = project.service<InsightsViewOrchestrator>()
        val historyScopeNavigation = project.service<HistoryScopeNavigation>()

        val miniScope = historyScopeNavigation.getPreviousScopeToGoBackTo()
        miniScope?.let {
            when (it.type) {
                ScopeType.Span -> {
                    insightViewOrch.showInsightsForCodelessSpan(it.id)
                }

                ScopeType.Method -> {
                    insightViewOrch.showInsightsForMethodFromBackNavigation(it.id)
                }

                else -> Unit
            }
        }

        if (miniScope == null) {
            project.service<HomeSwitcherService>().switchToHome()
        }
    }

}