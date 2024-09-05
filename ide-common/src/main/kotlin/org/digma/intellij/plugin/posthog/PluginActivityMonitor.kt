package org.digma.intellij.plugin.posthog

import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.DisabledPluginsState
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginStateListener
import com.intellij.ide.plugins.PluginStateManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.digma.intellij.plugin.PluginId
import org.digma.intellij.plugin.common.UniqueGeneratedUserId
import org.digma.intellij.plugin.docker.LocalInstallationFacade
import org.digma.intellij.plugin.log.Log

@Suppress("UnstableApiUsage")
@Service(Service.Level.PROJECT)
class PluginActivityMonitor(private val project: Project) : PluginStateListener, Disposable {


    private val myPluginId = com.intellij.openapi.extensions.PluginId.getId(PluginId.PLUGIN_ID)

    companion object {
        private val LOGGER = Logger.getInstance(PluginActivityMonitor::class.java)

        @JvmStatic
        fun getInstance(project: Project): PluginActivityMonitor {
            return project.service<PluginActivityMonitor>()
        }
    }

    init {
        PluginStateManager.addStateListener(this)


        val disabledListener = Runnable {
            if (DisabledPluginsState.getDisabledIds().contains(myPluginId)) {
                ActivityMonitor.getInstance(project).registerPluginDisabled()
                BrowserUtil.browse("https://digma.ai/disabled?u=${UniqueGeneratedUserId.userId}", project)
            }
        }

        DisabledPluginsState.addDisablePluginListener(disabledListener)
        Disposer.register(this) {
            DisabledPluginsState.removeDisablePluginListener(disabledListener)
        }

    }

    override fun uninstall(descriptor: IdeaPluginDescriptor) {
        if (descriptor.pluginId.idString == PluginId.PLUGIN_ID) {
            ActivityMonitor.getInstance(project).registerPluginUninstalled()
            BrowserUtil.browse("https://digma.ai/uninstall?u=${UniqueGeneratedUserId.userId}", project)


            if (service<LocalInstallationFacade>().isLocalEngineInstalled()) {
                Log.log(LOGGER::info, "removing digma engine on plugin uninstall")
                service<LocalInstallationFacade>().removeEngine(project) {
                    Log.log(LOGGER::info, "removed digma engine on plugin uninstall completed with {}", it)
                }
            }
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