package org.digma.intellij.plugin.ui.jcef.persistence

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.XMap
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.ui.jcef.createObjectMapper
import org.digma.intellij.plugin.ui.jcef.model.ErrorPayload
import org.digma.intellij.plugin.ui.jcef.model.GetFromPersistenceRequest
import org.digma.intellij.plugin.ui.jcef.model.SaveToPersistenceRequest
import org.digma.intellij.plugin.ui.jcef.model.Scope
import org.digma.intellij.plugin.ui.jcef.model.SetFromPersistenceMessage
import org.digma.intellij.plugin.ui.jcef.model.SetFromPersistenceMessagePayload
import org.digma.intellij.plugin.ui.jcef.serializeAndExecuteWindowPostMessageJavaScript

@Service(Service.Level.PROJECT)
class JCEFPersistenceService(private val project: Project) {

    companion object {
        @JvmStatic
        fun getInstance(project: Project): JCEFPersistenceService {
            return project.service<JCEFPersistenceService>()
        }
    }


    fun getFromPersistence(browser: CefBrowser, getFromPersistenceRequest: GetFromPersistenceRequest) {
        when (getFromPersistenceRequest.payload.scope) {
            Scope.application -> getFromApplicationPersistence(browser, getFromPersistenceRequest)
            Scope.project -> getFromProjectPersistence(browser, getFromPersistenceRequest)
        }
    }

    private fun getFromProjectPersistence(browser: CefBrowser, fromPersistenceRequest: GetFromPersistenceRequest) {
        try {
            val value = JCEFProjectPersistence.getInstance(project).get(fromPersistenceRequest.payload.key)
            sendPersistenceValue(browser, fromPersistenceRequest, value)
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "JCEFPersistenceService.getFromProjectPersistence", e)
            sendError(fromPersistenceRequest.payload.key, fromPersistenceRequest.payload.scope, e, browser)
        }
    }

    private fun getFromApplicationPersistence(browser: CefBrowser, fromPersistenceRequest: GetFromPersistenceRequest) {
        try {
            val value = JCEFApplicationPersistence.getInstance().get(fromPersistenceRequest.payload.key)
            sendPersistenceValue(browser, fromPersistenceRequest, value)
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "JCEFPersistenceService.getFromApplicationPersistence", e)
            sendError(fromPersistenceRequest.payload.key, fromPersistenceRequest.payload.scope, e, browser)
        }
    }


    private fun sendPersistenceValue(browser: CefBrowser, fromPersistenceRequest: GetFromPersistenceRequest, value: JsonNode?) {

        try {
            val message = SetFromPersistenceMessage(
                SetFromPersistenceMessagePayload(
                    fromPersistenceRequest.payload.key,
                    value,
                    fromPersistenceRequest.payload.scope
                )
            )
            serializeAndExecuteWindowPostMessageJavaScript(browser, message)
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "JCEFPersistenceService.sendPersistenceValue", e)
            sendError(fromPersistenceRequest.payload.key, fromPersistenceRequest.payload.scope, e, browser)
        }
    }


    private fun sendError(key: String, scope: Scope, e: Throwable, browser: CefBrowser) {
        val message = SetFromPersistenceMessage(
            SetFromPersistenceMessagePayload(
                key,
                null,
                scope, ErrorPayload(e.toString())
            )
        )
        serializeAndExecuteWindowPostMessageJavaScript(browser, message)
    }


    fun saveToPersistence(saveToPersistenceRequest: SaveToPersistenceRequest) {

        try {
            when (saveToPersistenceRequest.payload.scope) {
                Scope.application -> {
                    JCEFApplicationPersistence.getInstance().set(
                        saveToPersistenceRequest.payload.key,
                        saveToPersistenceRequest.payload.value
                    )
                }

                Scope.project -> {
                    JCEFProjectPersistence.getInstance(project).set(
                        saveToPersistenceRequest.payload.key,
                        saveToPersistenceRequest.payload.value
                    )

                }
            }
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "JCEFPersistenceService.saveToPersistence", e)
        }
    }
}


@State(
    name = "org.digma.intellij.plugin.ui.jcef.persistence.JCEFProjectPersistence",
    storages = [Storage("DigmaJCEFProjectPersistence.xml")]
)
@Service(Service.Level.PROJECT)
class JCEFProjectPersistence(val project: Project) : JCEFPersistence() {
    companion object {
        fun getInstance(project: Project): JCEFProjectPersistence {
            return project.service<JCEFProjectPersistence>()
        }
    }
}

@State(
    name = "org.digma.intellij.plugin.ui.jcef.persistence.JCEFApplicationPersistence",
    storages = [Storage("DigmaJCEFApplicationPersistence.xml")]
)
@Service(Service.Level.APP)
class JCEFApplicationPersistence : JCEFPersistence() {
    companion object {
        fun getInstance(): JCEFApplicationPersistence {
            return service<JCEFApplicationPersistence>()
        }
    }
}


abstract class JCEFPersistence : PersistentStateComponent<JCEFPersistenceState> {

    private var persistenceState = JCEFPersistenceState()

    override fun getState(): JCEFPersistenceState? {
        return persistenceState
    }

    override fun loadState(state: JCEFPersistenceState) {
        persistenceState = state
    }

    fun get(key: String): JsonNode? {
        return get(PersistenceKey(key))?.value
    }

    private fun get(key: PersistenceKey): PersistenceValue? {
        return persistenceState.state[key]
    }

    fun set(key: String, value: JsonNode) {
        set(PersistenceKey(key), PersistenceValue(value))
    }

    private fun set(key: PersistenceKey, value: PersistenceValue) {
        persistenceState.state[key] = value
    }

}

data class JCEFPersistenceState(
    @get:XMap
    val state: MutableMap<PersistenceKey, PersistenceValue> = mutableMapOf(),
)

data class PersistenceKey(
    @Attribute(value = "key")
    val key: String = "",
)

data class PersistenceValue(
    @Attribute(value = "filter", converter = JsonNodeConverter::class)
    val value: JsonNode? = null,
)


internal class JsonNodeConverter : Converter<JsonNode>() {

    private val objectMapper = createObjectMapper()

    override fun toString(value: JsonNode): String? {
        return objectMapper.writeValueAsString(value)
    }

    override fun fromString(value: String): JsonNode? {
        return objectMapper.readTree(value)
    }
}