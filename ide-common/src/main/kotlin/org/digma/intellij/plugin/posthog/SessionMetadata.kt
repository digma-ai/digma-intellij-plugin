package org.digma.intellij.plugin.posthog

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * hold data about current IDE session.
 */
@Service(Service.Level.APP)
class SessionMetadata {

    companion object {
        @JvmStatic
        fun getInstance(): SessionMetadata {
            return service<SessionMetadata>()
        }
    }

    private val metadata =
        mutableMapOf<SessionMetadataKey<*>, SessionMetadataKey.Value<*>>()


    fun <T : Any> put(key: SessionMetadataKey<T>, value: T) {
        metadata[key] = key.createValue(value)
    }

    fun <T : Any> getOrNull(key: SessionMetadataKey<T>): T? {
        return key.getValue(metadata)
    }

    fun <T : Any> get(key: SessionMetadataKey<T>): T {
        return key.getValue(metadata) ?: key.defaultValue
    }

    fun <T : Any> get(key: SessionMetadataKey<T>, defaultValue: T): T {
        return key.getValue(metadata) ?: defaultValue
    }

    fun remove(key: SessionMetadataKey<*>): Any? {
        return metadata.remove(key)?.theValue
    }

    fun getCreated(key: SessionMetadataKey<*>): Instant? {
        return key.getCreated(metadata)
    }

    fun getCreatedAsString(key: SessionMetadataKey<*>): String? {
        return key.getCreated(metadata)?.toString()
    }
}

/*
 * key must have a proper equals and hashCode
 */
data class SessionMetadataKey<T : Any>(val key: Any, val defaultValue: T) {

    companion object {
        @JvmStatic
        fun <T : Any> create(key: Any, defaultValue: T): SessionMetadataKey<T> {
            return SessionMetadataKey(key, defaultValue)
        }
    }

    fun createValue(theValue: T): Value<T> {
        return Value.create(theValue)
    }

    fun getValue(metadata: Map<SessionMetadataKey<*>, Value<*>>): T? {
        @Suppress("UNCHECKED_CAST")
        return metadata[this]?.theValue as T? //casting must succeed here
    }

    fun getCreated(metadata: Map<SessionMetadataKey<*>, Value<*>>): Instant? {
        return metadata[this]?.created //casting must succeed here
    }

    @Suppress("DataClassPrivateConstructor")
    data class Value<T>
    private constructor(val theValue: T) {
        val created = Clock.System.now()

        companion object {
            fun <T> create(theValue: T): Value<T> {
                return Value(theValue)
            }
        }
    }
}


fun getPluginLoadedKey(project: Project): SessionMetadataKey<Boolean> {
    return SessionMetadataKey.create("${project.name}-PluginLoadedKey", false)
}

fun getCurrentInstallStatusKey(): SessionMetadataKey<InstallStatus> {
    return SessionMetadataKey.create("CurrentInstallStatus", InstallStatus.Active)
}
