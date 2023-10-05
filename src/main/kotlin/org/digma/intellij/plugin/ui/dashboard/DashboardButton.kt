package org.digma.intellij.plugin.ui.dashboard

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.dashboard.DashboardService
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService.Companion.getInstance
import org.digma.intellij.plugin.ui.common.Laf
import java.awt.Cursor
import javax.swing.Icon
import javax.swing.JButton

class DashboardButton(val project: Project) : JButton() {

    private val logger: Logger = Logger.getInstance(DashboardButton::class.java)


    companion object {

        val dashboardIcon: Icon = Laf.Icons.Common.Dashboard

    }

    init {

        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        border = JBUI.Borders.empty()
        toolTipText = "Open Dashboard"
        isOpaque = false
        isContentAreaFilled = false
        isBorderPainted = false
        border = JBUI.Borders.empty()
        background = Laf.Colors.TRANSPARENT

        icon = dashboardIcon

        addActionListener {
            doActionListener()
        }
    }

    private fun doActionListener() {

        try {

            DashboardService.getInstance(project).openDashboard("Dashboard Panel - " + getInstance().state.currentEnv);

        } catch (e: Exception) {
            Log.warnWithException(logger, project, e, "Error in doActionListener")
            ErrorReporter.getInstance().reportError(project, "NotificationsButton.doActionListener", e)
        }
    }
}