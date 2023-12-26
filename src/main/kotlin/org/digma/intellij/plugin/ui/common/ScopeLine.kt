@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.Producer
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.insights.InsightsViewOrchestrator
import org.digma.intellij.plugin.model.discovery.CodeLessSpan
import org.digma.intellij.plugin.model.discovery.EndpointInfo
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.nav.ScopeType
import org.digma.intellij.plugin.navigation.HistoryScopeNavigation
import org.digma.intellij.plugin.navigation.HomeSwitcherService
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel


fun scopeLine(
    project: Project,
    scopeNameProducer: Producer<String>,
    scopeTooltipProducer: Producer<String>,
    scopeIconProducer: Producer<Icon>,
): DigmaResettablePanel {


    val navButton = BackNavButton()
    val size = Laf.scaleSize(Laf.Sizes.BUTTON_SIZE_24)
    val buttonsSize = Dimension(size, size)
    navButton.preferredSize = buttonsSize
    navButton.maximumSize = buttonsSize
    navButton.addActionListener {

        val insightViewOrch = project.service<InsightsViewOrchestrator>()
        val historyScopeNavigation = project.service<HistoryScopeNavigation>()

        val miniScope = historyScopeNavigation.getPreviousScopeToGoBackTo()
        miniScope?.let {
            when (it.type) {
                ScopeType.Span -> {
                    insightViewOrch.showInsightsForCodelessSpan(it.scopeObject as CodeLessSpan)
                }

                ScopeType.Method -> {
                    insightViewOrch.showInsightsForMethodFromBackNavigation(it.scopeObject as MethodInfo)
                }

                ScopeType.Endpoint -> {
                    insightViewOrch.showInsightsForEndpointFromBackNavigation(it.scopeObject as EndpointInfo)
                }

                else -> Unit
            }
        }

        if (miniScope == null) {
            project.service<HomeSwitcherService>().switchToHome()
        }
    }


    val scopeIcon = JLabel(scopeIconProducer.produce())
    val scopeLabel = JLabel(scopeNameProducer.produce())
    scopeLabel.toolTipText = scopeTooltipProducer.produce()


    val mainPanel = object : DigmaResettablePanel() {
        override fun reset() {
            scopeIcon.icon = scopeIconProducer.produce()
            scopeLabel.text = scopeNameProducer.produce()
            scopeLabel.toolTipText = scopeTooltipProducer.produce()
        }
    }


    mainPanel.layout = BorderLayout(5, 0)
    mainPanel.isOpaque = false
    mainPanel.add(navButton, BorderLayout.WEST)
    val centerPnel = JPanel(BorderLayout(5, 0))
    centerPnel.isOpaque = false
    centerPnel.border = JBUI.Borders.empty()
    centerPnel.add(scopeIcon, BorderLayout.WEST)
    centerPnel.add(scopeLabel, BorderLayout.CENTER)
    mainPanel.add(centerPnel, BorderLayout.CENTER)

    return mainPanel

}
