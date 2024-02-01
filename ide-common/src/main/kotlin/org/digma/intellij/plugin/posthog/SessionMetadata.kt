package org.digma.intellij.plugin.posthog

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant


@Service(Service.Level.APP)
class SessionMetadata {

    companion object {
        @JvmStatic
        fun getInstance(): SessionMetadata {
            return service<SessionMetadata>()
        }
    }

    private val metadata = mutableMapOf<SessionMetadataKey, SessionMetadataValue>()

    fun put(key: Any, value: Any) {
        put(SessionMetadataKey.create(key), value)
    }

    fun put(key: SessionMetadataKey, value: Any) {
        metadata[key] = SessionMetadataValue.create(value)
    }

    fun get(key: Any): Any? {
        return get(SessionMetadataKey.create(key))
    }

    fun get(key: SessionMetadataKey): Any? {
        return metadata[key]?.value
    }

    fun remove(key: Any): Any? {
        return remove(SessionMetadataKey.create(key))
    }

    fun remove(key: SessionMetadataKey): Any? {
        return metadata.remove(key)?.value
    }

    fun getCreated(key: Any): Instant? {
        return getCreated(SessionMetadataKey.create(key))
    }

    fun getCreated(key: SessionMetadataKey): Instant? {
        return metadata[key]?.created
    }
}

/*
 * key must have a proper equals and hashCode
 */
data class SessionMetadataKey(val key: Any) {

    companion object {
        @JvmStatic
        fun create(key: Any): SessionMetadataKey {
            return SessionMetadataKey(key)
        }
    }
}

private data class SessionMetadataValue(val value: Any) {

    val created = Clock.System.now()

    companion object {
        fun create(value: Any): SessionMetadataValue {
            return SessionMetadataValue(value)
        }
    }
}


fun getPluginLoadedKey(project: Project): SessionMetadataKey {
    return SessionMetadataKey.create("${project.name}-PluginLoadedKey")
}
