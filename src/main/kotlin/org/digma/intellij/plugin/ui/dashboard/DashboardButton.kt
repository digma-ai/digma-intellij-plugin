package org.digma.intellij.plugin.ui.dashboard

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.analytics.EnvironmentChanged
import org.digma.intellij.plugin.common.adjustEnvironmentDisplayName
import org.digma.intellij.plugin.dashboard.DashboardService
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.ui.common.Laf
import java.awt.Cursor
import javax.swing.JButton

class DashboardButton(val project: Project) : JButton() {

    private val logger: Logger = Logger.getInstance(DashboardButton::class.java)


    init {

        icon = if (JBColor.isBright()) Laf.Icons.Common.DashboardLight else Laf.Icons.Common.DashboardDark
        pressedIcon = if (JBColor.isBright()) Laf.Icons.Common.DashboardLightPressed else Laf.Icons.Common.DashboardDarkPressed
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        border = JBUI.Borders.empty()
        toolTipText = "Open Dashboard"
        isOpaque = false
        isContentAreaFilled = false
        isBorderPainted = false
        border = JBUI.Borders.empty()
        background = Laf.Colors.TRANSPARENT


        //on fresh install there is no env yet
        if (PersistenceService.getInstance().getCurrentEnv() == null) {
            isEnabled = false
            toolTipText = "No Environment Yet"
            project.messageBus.connect().subscribe(
                EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC,
                object : EnvironmentChanged {

                    override fun environmentChanged(newEnv: String?, refreshInsightsView: Boolean) {
                        isEnabled = PersistenceService.getInstance().getCurrentEnv() != null
                        toolTipText = if (isEnabled) "Open Dashboard" else "No Environment Yet"
                    }

                    override fun environmentsListChanged(newEnvironments: MutableList<String>?) {
                        isEnabled = PersistenceService.getInstance().getCurrentEnv() != null
                        toolTipText = if (isEnabled) "Open Dashboard" else "No Environment Yet"
                    }
                })
        }


        addActionListener {
            doActionListener()
        }
    }

    private fun doActionListener() {

        try {

            val envName = PersistenceService.getInstance().getCurrentEnv()?.let {
                adjustEnvironmentDisplayName(it)
            } ?: "No Environment"

            DashboardService.getInstance(project).openDashboard("Dashboard Panel - $envName");

        } catch (e: Exception) {
            Log.warnWithException(logger, project, e, "Error in doActionListener")
            ErrorReporter.getInstance().reportError(project, "DashboardButton.doActionListener", e)
        }
    }
}