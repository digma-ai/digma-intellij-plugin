package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.launchBackground
import com.intellij.openapi.ui.DialogPanel
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.ui.JBUI
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import org.digma.intellij.plugin.common.modelChangeListener.ModelChangeListener
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.refreshInsightsTask.RefreshService
import org.digma.intellij.plugin.ui.errors.GeneralRefreshIconButton
import org.digma.intellij.plugin.ui.model.PanelModel
import org.digma.intellij.plugin.ui.model.errors.ErrorsModel
import org.digma.intellij.plugin.ui.model.insights.InsightsModel
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import java.awt.Cursor
import java.awt.Dimension
import java.util.concurrent.locks.ReentrantLock
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent

private const val REFRESH_ALL_INSIGHTS_AND_ERRORS = "Refresh"

class ScopeLineResultPanel(
        project: Project,
        model: PanelModel,
): DigmaTabPanel(), Disposable {
    private val logger: Logger = Logger.getInstance(ScopeLineResultPanel::class.java)

    private val modelChangeConnection: MessageBusConnection = project.messageBus.connect()
    private val project: Project
    private val model: PanelModel
    private val rebuildPanelLock = ReentrantLock()
    private var scopeLine: DialogPanel? = null
    private var refreshService: RefreshService

    init {
        modelChangeConnection.subscribe(
                ModelChangeListener.MODEL_CHANGED_TOPIC,
                ModelChangeListener { newModel -> rebuildInBackground(newModel) }
        )
        this.refreshService = project.getService(RefreshService::class.java)
        this.project = project
        this.model = model
        this.layout = BoxLayout(this, BoxLayout.LINE_AXIS)
        this.border = JBUI.Borders.emptyLeft(5)
        this.background = Laf.Colors.NAVIGATION_TOP_BACKGROUND_DARK
        this.isOpaque = true

        rebuildInBackground(model)
    }

    override fun dispose() {
        modelChangeConnection.dispose()
    }

    override fun getPreferredFocusableComponent(): JComponent {
        return this
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return this
    }

    override fun reset() {
        rebuildInBackground(model)
    }

    private fun rebuildInBackground(model: PanelModel) {
        val lifetimeDefinition = LifetimeDefinition()
        lifetimeDefinition.lifetime.launchBackground {
            rebuildPanelLock.lock()
            Log.log(logger::debug, "Lock acquired for rebuild ScopeLineResultPanel process.")
            try {
                rebuild(model)
            } finally {
                rebuildPanelLock.unlock()
                Log.log(logger::debug, "Lock released for rebuild ScopeLineResultPanel process.")
                lifetimeDefinition.terminate()
            }
        }
    }

    private fun rebuild(model: PanelModel) {
        ApplicationManager.getApplication().invokeLater {
            removeExistingComponentsIfPresent()
            buildScopeLineResultPanelComponents(model)
            revalidate()
        }
    }

    private fun removeExistingComponentsIfPresent() {
        if (components.isNotEmpty()) {
            this.components.forEach {
                this.remove(it)
            }
        }
    }

    private fun buildScopeLineResultPanelComponents(model: PanelModel) {
        if (project.isDisposed) {
            return
        }
        if (model is InsightsModel || model is ErrorsModel) {
            scopeLine = scopeLine({ model.getScope() }, { model.getScopeTooltip() }, ScopeLineIconProducer(model))
            scopeLine!!.isOpaque = false
            scopeLine!!.border = JBUI.Borders.empty(2, 4)
        }
        this.add(scopeLine)
        this.add(Box.createHorizontalGlue())
        this.add(getGeneralRefreshButton(project))
    }

    private fun getGeneralRefreshButton(project: Project): JButton {
        val size = Laf.scalePanels(Laf.Sizes.BUTTON_SIZE_24)
        val buttonsSize = Dimension(size, size)
        val generalRefreshIconButton = GeneralRefreshIconButton(project, Laf.Icons.Insight.REFRESH)
        generalRefreshIconButton.preferredSize = buttonsSize
        generalRefreshIconButton.maximumSize = buttonsSize
        generalRefreshIconButton.toolTipText = asHtml(REFRESH_ALL_INSIGHTS_AND_ERRORS)
        generalRefreshIconButton.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

        generalRefreshIconButton.addActionListener {
            refreshService.refreshAllInBackground()
        }
        return generalRefreshIconButton
    }
}