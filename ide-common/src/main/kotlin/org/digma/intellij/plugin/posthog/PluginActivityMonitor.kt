package org.digma.intellij.plugin.posthog

import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginStateListener
import com.intellij.ide.plugins.PluginStateManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.PluginId
import org.digma.intellij.plugin.log.Log

class PluginActivityMonitor(private val project: Project) : PluginStateListener, Disposable {
    companion object {
        private val logger = Logger.getInstance(PluginActivityMonitor::class.java)
        @JvmStatic
        fun loadInstance(project: Project) {
            Log.test(logger,"Getting instance of ${PluginActivityMonitor::class.simpleName}")
            project.getService(PluginActivityMonitor::class.java)
            Log.test(logger,"Returning ${PluginActivityMonitor::class.simpleName}")
        }
    }

    init {
        Log.test(logger, "Initializing ${PluginActivityMonitor::class.simpleName}")
        PluginStateManager.addStateListener(this)
        Log.test(logger, "Finished ${PluginActivityMonitor::class.simpleName} initialization")
    }

    override fun uninstall(descriptor: IdeaPluginDescriptor) {
        if(descriptor.pluginId.idString == PluginId.PLUGIN_ID){
            ActivityMonitor.getInstance(project).registerPluginUninstalled()
        }
        BrowserUtil.browse("https://digma.ai/uninstall/", project)
        super.uninstall(descriptor)
    }

    override fun install(descriptor: IdeaPluginDescriptor) {
        // nothing
    }

    override fun dispose() {
        PluginStateManager.removeStateListener(this)
    }
}