package org.digma.intellij.plugin.ui.common

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.launchBackground
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.util.Alarm
import com.intellij.util.AlarmFactory
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.ui.JBUI
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceConnectionEvent
import org.digma.intellij.plugin.common.CommonUtils
import org.digma.intellij.plugin.common.modelChangeListener.ModelChangeListener
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.model.PanelModel
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsSupplier
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.locks.ReentrantLock
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants


class NavigationPanel(
        project: Project,
        model: PanelModel,
        private val environmentsSupplier: EnvironmentsSupplier // assuming its a singleton
) : DigmaResettablePanel(), Disposable {
    private val logger: Logger = Logger.getInstance(NavigationPanel::class.java)

    private val messageBusConnection: MessageBusConnection = project.messageBus.connect()
    private val project: Project
    private val model: PanelModel
    private val changeEnvAlarm: Alarm
    private val localHostname: String
    private var analyticsService: AnalyticsService? = null
    private val rebuildPanelLock = ReentrantLock()
    private var environmentsDropdownPanel:EnvironmentsDropdownPanel ? = null


    private var myScopeLineResultPanel: ScopeLineResultPanel? = null

    init {
        this.project = project
        this.model = model
        changeEnvAlarm = AlarmFactory.getInstance().create()
        localHostname = CommonUtils.getLocalHostname()
        isOpaque = false
        layout = BorderLayout()
        border = JBUI.Borders.empty()
        analyticsService = project.getService(AnalyticsService::class.java)

        rebuildInBackground(model)

        messageBusConnection.subscribe(AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC, object : AnalyticsServiceConnectionEvent {
            override fun connectionLost() {
                rebuildInBackground(model)
            }

            override fun connectionGained() {
                rebuildInBackground(model)
            }
        })
        messageBusConnection.subscribe(
                ModelChangeListener.MODEL_CHANGED_TOPIC,
                        object : ModelChangeListener {
                            override fun modelChanged(newModel: PanelModel) {
                                rebuildInBackground(newModel)
                            }
                        }
        )
    }


    override fun reset() {
        rebuildInBackground(model)
    }

    private fun rebuildInBackground(model: PanelModel) {
        val lifetimeDefinition = LifetimeDefinition()
        lifetimeDefinition.lifetime.launchBackground {
            rebuildPanelLock.lock()
            Log.log(logger::debug, "Lock acquired for rebuild Navigation panel process.")
            try {
                rebuild(model)
            } finally {
                rebuildPanelLock.unlock()
                Log.log(logger::debug, "Lock released for rebuild Navigation panel process.")
                lifetimeDefinition.terminate()
            }
        }
    }

    private fun rebuild(model: PanelModel) {
        ApplicationManager.getApplication().invokeLater {
            if (!project.isDisposed) {
                removeExistingComponentsIfPresent()
                buildNavigationPanelComponents(model)
                revalidate()
            }
        }
    }

    private fun buildNavigationPanelComponents(model: PanelModel) {

        val mainPanel = JPanel(BorderLayout())
        mainPanel.background = Laf.Colors.EDITOR_BACKGROUND

        val topPanel = JPanel(BorderLayout(5,0))
        topPanel.background = Laf.Colors.EDITOR_BACKGROUND
        topPanel.border = JBUI.Borders.empty()
        val logo = getLogoIconLabel()
        logo.alignmentX = 0F
        topPanel.add(logo,BorderLayout.WEST)
        val firstRow = getFirstRowPanel(model)
        firstRow.alignmentX = 1F
        topPanel.add(firstRow,BorderLayout.CENTER)
        mainPanel.add(topPanel,BorderLayout.NORTH)

        val bottomPanel = JPanel(BorderLayout(5,0))
        bottomPanel.background = Laf.Colors.EDITOR_BACKGROUND
        bottomPanel.border = JBUI.Borders.empty()
        val cardsPanel = getSecondRowPanel()
        val homeButton = getHomeButton(cardsPanel)
        homeButton.alignmentX = 0F
        bottomPanel.add(homeButton,BorderLayout.WEST)
        cardsPanel.alignmentX = 1F
        bottomPanel.add(cardsPanel,BorderLayout.CENTER)
        mainPanel.add(bottomPanel,BorderLayout.SOUTH)

        add(mainPanel,BorderLayout.CENTER)
    }

    private fun removeExistingComponentsIfPresent() {
        if (components.isNotEmpty()) {
            this.components.forEach {
                this.remove(it)
            }
        }
    }

    private fun getFirstRowPanel(model: PanelModel): JPanel {

        environmentsDropdownPanel?.dispose()
        environmentsDropdownPanel = EnvironmentsDropdownPanel(project, model, environmentsSupplier, localHostname)

        val parentPanel = JPanel(BorderLayout())
        parentPanel.add(environmentsDropdownPanel, BorderLayout.CENTER)
        parentPanel.add(getSettingsButton(), BorderLayout.EAST)
        parentPanel.background = Laf.Colors.EDITOR_BACKGROUND

        return parentPanel
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
        cardsPanel.add(scopeLine,HomeButton.SCOPE_LINE_PANEL)
        cardsPanel.add(projectPanel,HomeButton.HOME_PROJECT_PANEL)
        cardsLayout.addLayoutComponent(scopeLine,HomeButton.SCOPE_LINE_PANEL)
        cardsLayout.addLayoutComponent(projectPanel,HomeButton.HOME_PROJECT_PANEL)
        cardsLayout.show(cardsPanel,HomeButton.SCOPE_LINE_PANEL)

        return cardsPanel
    }


    fun getHomeButton(cardsPanel: JPanel):JComponent{
        val homeButton = HomeButton(project,cardsPanel)
        val size = Laf.scalePanels(Laf.Sizes.BUTTON_SIZE_24)
        val buttonsSize = Dimension(size, size)
        homeButton.preferredSize = buttonsSize
        homeButton.maximumSize = buttonsSize
        return homeButton
    }



    private fun createScopeLinePanel():JPanel{
        val panel = JPanel(BorderLayout())
        panel.isOpaque = false
        panel.border = JBUI.Borders.empty()
        myScopeLineResultPanel?.dispose()
        myScopeLineResultPanel = ScopeLineResultPanel(project, model)
        panel.add(myScopeLineResultPanel!!,BorderLayout.CENTER)
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

    private fun getLogoIconLabel(): JComponent {
        val logoIconLabel = JBLabel(Laf.Icons.General.DIGMA_LOGO,SwingConstants.CENTER)
        val size = Laf.scalePanels(Laf.Sizes.BUTTON_SIZE_24)
        val buttonsSize = Dimension(size, size)
        logoIconLabel.preferredSize = buttonsSize
        logoIconLabel.maximumSize = buttonsSize
        return logoIconLabel
    }


    private fun getSettingsButton(): JPanel {
        val iconLabel = JLabel(Laf.Icons.Insight.THREE_DOTS, SwingConstants.RIGHT)
        iconLabel.horizontalAlignment = SwingConstants.RIGHT
        iconLabel.verticalAlignment = SwingConstants.TOP
        iconLabel.isOpaque = false
        iconLabel.border = JBUI.Borders.empty(2, 5)
        iconLabel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        iconLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                showSettingsMessage(
                        threeDotsIcon = iconLabel,
                        project = project
                )
            }

            override fun mouseExited(e: MouseEvent?) {}
            override fun mousePressed(e: MouseEvent?) {}
        })

        val wrapper = JPanel()
        wrapper.isOpaque = false
        wrapper.layout = FlowLayout(FlowLayout.CENTER, 10, 5)
        wrapper.add(iconLabel)
        return wrapper
    }

    private fun showSettingsMessage(threeDotsIcon: JLabel, project: Project) {
        HintManager.getInstance().showHint(SettingsHintPanel(project), RelativePoint.getSouthWestOf(threeDotsIcon), HintManager.HIDE_BY_ESCAPE, 5000)
    }

    override fun dispose() {
        messageBusConnection.dispose()
    }
}