package org.digma.intellij.plugin.session

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.digma.intellij.plugin.posthog.InstallStatus


const val CURRENT_INSTALL_STATUS_KEY = "CURRENT_INSTALL_STATUS_KEY"
const val LAST_LENS_CLICKED_KEY = "LAST_LENS_CLICKED_KEY"
const val LATEST_UNKNOWN_RUN_CONFIG_TASKS_KEY = "LATEST_UNKNOWN_RUN_CONFIG_TASKS_KEY"
const val LAST_INSIGHTS_VIEWED_KEY = "LAST_INSIGHTS_VIEWED_KEY"


/**
 * in memory properties for the current IDE session.
 * Keeps time of properties insert, the last time a property was changed if exists.
 * can be used in a type safe call as:
 * SessionMetadataProperties.getInstance().get<InstallStatus>(CURRENT_INSTALL_STATUS_KEY)
 */
@Service(Service.Level.APP)
class SessionMetadataProperties {

    val metadataProperties = mutableMapOf<Any, SessionMetadataProperty>()

    companion object {
        @JvmStatic
        fun getInstance(): SessionMetadataProperties {
            return service<SessionMetadataProperties>()
        }
    }


    //defaultValue value should never be null
    inline fun <reified T> get(key: Any, defaultValue: T): T {
        return metadataProperties[key]?.value as T? ?: defaultValue
    }

    //if key doesn't exist put defaultValue in the map and return defaultValue.
    //defaultValue should never be null
    inline fun <reified T : Any> getAndPutDefault(key: Any, defaultValue: T): T {
        return metadataProperties.computeIfAbsent(key) {
            SessionMetadataProperty(defaultValue)
        }.value as T
    }

    inline fun <reified T> get(key: Any): T? {
        return metadataProperties[key]?.value as T?
    }

    fun put(key: Any, value: Any) {
        metadataProperties[key] = SessionMetadataProperty(value)
    }

    inline fun <reified T> remove(key: Any): T? {
        return metadataProperties.remove(key) as T?
    }

    fun getCreated(key: Any): Instant? {
        return metadataProperties[key]?.created
    }

    fun getCreatedAsString(key: Any): String? {
        return metadataProperties[key]?.created?.toString()
    }

    class SessionMetadataProperty(val value: Any) {
        val created = Clock.System.now()
    }

}


//service methods for ease of use, usually properties that have a default value or special key

fun getPluginLoadedKey(project: Project): String {
    return "${project.name}-PluginLoadedKey"
}

fun getCurrentInstallStatus(): InstallStatus {
    return SessionMetadataProperties.getInstance().get<InstallStatus>(CURRENT_INSTALL_STATUS_KEY, InstallStatus.Active)
}