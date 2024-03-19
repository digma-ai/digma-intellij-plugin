package org.digma.intellij.plugin.posthog

import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.settings.SettingsState

class SettingsChangeTracker {

    private val myTrackedSettings = mutableMapOf<String,String>()

    init {
        updateTrackedSettings()
    }

    fun start(activityMonitor: ActivityMonitor) {

        activityMonitor.registerSettingsEvent("initial", myTrackedSettings)

        SettingsState.getInstance().addChangeListener(
            {
                Backgroundable.executeOnPooledThread {
                    val oldSettings = mutableMapOf<String, String>()
                    oldSettings.putAll(myTrackedSettings)
                    updateTrackedSettings()
                    val diff = getDiff(oldSettings, myTrackedSettings)
                    activityMonitor.registerSettingsEvent("change", diff)
                }
            },
            activityMonitor
        )
    }

    private fun getDiff(oldSettings: MutableMap<String, String>, myTrackedSettings: MutableMap<String, String>): Map<String,String> {

        val diffMap = mutableMapOf<String,String>()
        oldSettings.forEach{
            val key = it.key
            val value = it.value
            if (value != myTrackedSettings[key]){
                diffMap[key] = "[new value ${myTrackedSettings[key]}] [old value $value]"
            }
        }
        return diffMap
    }


    private fun updateTrackedSettings(){
        myTrackedSettings["apiUrl"] = SettingsState.getInstance().apiUrl
        myTrackedSettings["jaegerQueryUrl"] = SettingsState.getInstance().jaegerQueryUrl
        myTrackedSettings["jaegerUrl"] = SettingsState.getInstance().jaegerUrl.toString()
        myTrackedSettings["jaegerLinkMode"] = SettingsState.getInstance().jaegerLinkMode.name
        myTrackedSettings["refreshDelay"] = SettingsState.getInstance().refreshDelay.toString()
        myTrackedSettings["springBootObservabilityMode"] = SettingsState.getInstance().springBootObservabilityMode.name
        myTrackedSettings["runtimeObservabilityBackendUrl"] = SettingsState.getInstance().runtimeObservabilityBackendUrl
        myTrackedSettings["extendedObservability"] = SettingsState.getInstance().extendedObservability.toString()
    }



}

