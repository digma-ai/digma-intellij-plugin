package org.digma.intellij.plugin.posthog

import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginStateListener
import com.intellij.ide.plugins.PluginStateManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.PluginId

class PluginActivityMonitor(private val project: Project) : PluginStateListener, Disposable {
    companion object {
        private val LOGGER = Logger.getInstance(PluginActivityMonitor::class.java)

        @JvmStatic
        fun loadInstance(project: Project) {
            project.getService(PluginActivityMonitor::class.java)
        }
    }

    init {
        PluginStateManager.addStateListener(this)
    }

    override fun uninstall(descriptor: IdeaPluginDescriptor) {
        if(descriptor.pluginId.idString == PluginId.PLUGIN_ID){
            val userId = ActivityMonitor.getInstance(project).registerPluginUninstalled()
            BrowserUtil.browse("https://digma.ai/uninstall?u=$userId", project)
        }
        super.uninstall(descriptor)
    }

    override fun install(descriptor: IdeaPluginDescriptor) {
        // nothing
    }

    override fun dispose() {
        PluginStateManager.removeStateListener(this)
    }
}