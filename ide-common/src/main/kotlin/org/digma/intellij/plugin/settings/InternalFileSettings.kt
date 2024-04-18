package org.digma.intellij.plugin.settings

import java.util.Properties

object InternalFileSettings {

    //todo : add a way to inject properties in gradle build, for example in github

    private val properties: Properties? = try {
        val properties = Properties()
        properties.load(this::class.java.getResourceAsStream("/digma-settings.properties"))
        properties
    } catch (e: Throwable) {
        null
    }

    private fun getProperty(key: String, defaultValue: String? = null): String? {
        return System.getProperty(key) ?: properties?.getProperty(key) ?: defaultValue
    }


    fun getAllSettingsOf(prefix: String): MutableMap<Any, Any> {
        val fromSystemProperties = System.getProperties().filter { it.key.toString().startsWith(prefix, true) }
        val fromFile = properties?.filter { it.key.toString().startsWith(prefix, true) }?.toMutableMap()
        val result = fromFile?.toMutableMap() ?: mutableMapOf()
        result.putAll(fromSystemProperties)
        return result
    }


    fun getAggressiveUpdateServiceEnabled(): Boolean {
        return getProperty("AggressiveUpdateService.enabled")?.toBoolean() ?: true
    }

    fun getAggressiveUpdateServiceMinimalBackendVersion(): String? {
        return getProperty("AggressiveUpdateService.minimal.backend.version")
    }

    fun getAggressiveUpdateServiceMonitorDelaySeconds(defaultValue: Int): Int {
        return getProperty("AggressiveUpdateService.monitor.delay.seconds")?.toInt() ?: defaultValue
    }

    fun getAggressiveUpdateNotificationsDelayMinutes(defaultValue: Int): Int {
        return getProperty("AggressiveUpdateService.notification.delay.minutes")?.toInt() ?: defaultValue
    }

    fun getUpdateServiceMonitorDelaySeconds(defaultValue: Int): Int {
        return getProperty("UpdateService.monitor.delay.seconds")?.toInt() ?: defaultValue
    }


}