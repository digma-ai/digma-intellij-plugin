package org.digma.intellij.plugin.recentactivity

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
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


    companion object {
        @JvmStatic
        fun getInstance(project: Project): RecentActivityToolWindowShower {
            return project.service<RecentActivityToolWindowShower>()
        }
    }


    fun isToolWindowVisible(): Boolean {
        if (toolWindow != null) {
            return toolWindow!!.isVisible
        }
        return false
    }


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

    fun hideToolWindow() {
        Log.log(logger::trace, "hideToolWindow invoked")

        EDT.ensureEDT {
            if (toolWindow != null) {
                toolWindow?.let {
                    Log.log(logger::trace, "Got reference to tool window, hiding..")
                    hide(it)
                }

            } else {
                Log.log(logger::trace, "Don't have reference to tool window, hiding with ToolWindowManager..")
                val tw: ToolWindow? = ToolWindowManager.getInstance(project).getToolWindow(PluginId.OBSERVABILITY_WINDOW_ID)

                tw?.let {
                    Log.log(logger::trace, "Got tool window from ToolWindowManager")
                    hide(tw)
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

    private fun hide(toolWindow: ToolWindow) {
        if (!toolWindow.isVisible) {
            Log.log(logger::trace, "Tool window is already hidden")
        } else {
            Log.log(logger::trace, "Calling toolWindow.show")
            toolWindow.hide()
        }
    }

}