package org.digma.intellij.plugin.ui.recentactivity

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import org.digma.intellij.plugin.PluginId
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.log.Log

@Service(Service.Level.PROJECT)
class RecentActivityToolWindowShower(val project: Project) {

    private val logger = Logger.getInstance(this::class.java)

    var toolWindow: ToolWindow? = null

    fun showToolWindow() {
        Log.log(logger::trace, "showToolWindow invoked")

        EDT.ensureEDT {
            if (toolWindow != null) {
                toolWindow?.let {
                    Log.log(logger::trace, "Got reference to tool window, showing..")
                    show(it)
                }

            } else {
                Log.log(logger::trace, "Don't have reference to tool window, showing with ToolWindowManager..")
                val tw: ToolWindow? = ToolWindowManager.getInstance(project).getToolWindow(PluginId.OBSERVABILITY_WINDOW_ID)

                tw?.let {
                    Log.log(logger::trace, "Got tool window from ToolWindowManager")
                    show(tw)
                } ?: Log.log(logger::trace, "Could not find tool window")
            }
        }
    }


    private fun show(toolWindow: ToolWindow) {
        if (toolWindow.isVisible) {
            Log.log(logger::trace, "Tool window is already visible")
        } else {
            Log.log(logger::trace, "Calling toolWindow.show")
            toolWindow.show()
        }
    }

}