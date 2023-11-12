package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.dashboard.DashboardButton
import org.digma.intellij.plugin.ui.notifications.NotificationsButton
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel


class NavigationPanel(private val project: Project) : JPanel() {


    init {
        isOpaque = false
        layout = BorderLayout()
        border = JBUI.Borders.empty()
        buildNavigationPanelComponents()
    }


    private fun buildNavigationPanelComponents() {

        val mainPanel = JPanel(BorderLayout())
        mainPanel.background = Laf.Colors.EDITOR_BACKGROUND

        val topPanel = JPanel(BorderLayout(5, 0))
        topPanel.background = Laf.Colors.EDITOR_BACKGROUND
        topPanel.border = JBUI.Borders.empty()

        val cardsPanel = getSecondRowPanel()
        cardsPanel.alignmentX = 1F

        val homeButton = getHomeButton(cardsPanel)
        homeButton.alignmentX = 0F
        topPanel.add(homeButton, BorderLayout.WEST)
        val firstRow = getFirstRowPanel()
        firstRow.alignmentX = 1F
        topPanel.add(firstRow, BorderLayout.CENTER)
        mainPanel.add(topPanel, BorderLayout.NORTH)

        val bottomPanel = JPanel(BorderLayout(5, 0))
        bottomPanel.background = Laf.Colors.EDITOR_BACKGROUND
        bottomPanel.border = JBUI.Borders.empty()
        bottomPanel.add(cardsPanel, BorderLayout.CENTER)
        mainPanel.add(bottomPanel, BorderLayout.SOUTH)

        add(mainPanel, BorderLayout.CENTER)
    }


    private fun getFirstRowPanel(): JPanel {
        val parentPanel = JPanel(BorderLayout())
        parentPanel.add(EnvironmentsCombo(project, this), BorderLayout.CENTER)
        parentPanel.add(getFirstRowButtons(), BorderLayout.EAST)
        parentPanel.background = Laf.Colors.EDITOR_BACKGROUND
        return parentPanel
    }


    private fun getFirstRowButtons(): JPanel {

        val buttonsPanel = JPanel(GridLayout(1, 3, 5, 0))
        buttonsPanel.border = JBUI.Borders.empty()
        buttonsPanel.background = Laf.Colors.EDITOR_BACKGROUND
        buttonsPanel.add(getDashboardButton())
        buttonsPanel.add(getNotificationsButton())
        buttonsPanel.add(getSettingsButton())
        return buttonsPanel
    }




    private fun getSecondRowPanel(): JPanel {

        val scopeLine = createScopeLinePanel()
        val projectPanel = createProjectPanel()

        val cardsLayout = CardLayout()
        val cardsPanel = JPanel(cardsLayout)
        cardsPanel.border = JBUI.Borders.emptyRight(4)
        cardsPanel.background = Laf.Colors.EDITOR_BACKGROUND

        cardsPanel.isOpaque = false
        cardsPanel.border = JBUI.Borders.empty()
        cardsPanel.add(scopeLine, HomeButton.SCOPE_LINE_PANEL)
        cardsPanel.add(projectPanel, HomeButton.HOME_PROJECT_PANEL)
        cardsLayout.addLayoutComponent(scopeLine, HomeButton.SCOPE_LINE_PANEL)
        cardsLayout.addLayoutComponent(projectPanel, HomeButton.HOME_PROJECT_PANEL)
        cardsLayout.show(cardsPanel, HomeButton.SCOPE_LINE_PANEL)

        return cardsPanel
    }


    private fun getHomeButton(cardsPanel: JPanel): JComponent {
        val homeButton = HomeButton(project, cardsPanel)
        val size = Laf.scalePanels(Laf.Sizes.BUTTON_SIZE_24)
        val buttonsSize = Dimension(size, size)
        homeButton.preferredSize = buttonsSize
        homeButton.maximumSize = buttonsSize
        return homeButton
    }


    private fun createScopeLinePanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.isOpaque = false
        panel.border = JBUI.Borders.empty()
        panel.add(ScopeLinePanel(project), BorderLayout.CENTER)
        return panel
    }

    private fun createProjectPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.isOpaque = false
        panel.border = JBUI.Borders.empty()
        val projectPanel = ProjectHomePanel(this.project)
        panel.add(projectPanel, BorderLayout.CENTER)
        return panel
    }

    private fun getSettingsButton(): JPanel {
        val quickSettingsButton = QuickSettingsButton(project)
        val wrapper = JPanel()
        wrapper.isOpaque = false
        wrapper.layout = FlowLayout(FlowLayout.CENTER, 10, 5)
        wrapper.add(quickSettingsButton)
        return wrapper
    }

    private fun getNotificationsButton(): JButton {

        val size = Laf.scalePanels(Laf.Sizes.BUTTON_SIZE_24)
        val buttonsSize = Dimension(size, size)
        val notificationsButton = NotificationsButton(project)
        notificationsButton.preferredSize = buttonsSize
        notificationsButton.maximumSize = buttonsSize
        return notificationsButton
    }

    private fun getDashboardButton(): JButton {

        val size = Laf.scalePanels(Laf.Sizes.BUTTON_SIZE_24)
        val buttonsSize = Dimension(size, size)
        val dashboardButton = DashboardButton(project)
        dashboardButton.preferredSize = buttonsSize
        dashboardButton.maximumSize = buttonsSize
        return dashboardButton
    }
}