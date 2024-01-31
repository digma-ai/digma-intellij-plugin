package org.digma.intellij.plugin.ui.jcef.persistence

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.errorreporting.ErrorReporter
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
        val value = JCEFProjectPersistence.getInstance(project).get(PersistenceKey(fromPersistenceRequest.payload.key))
        sendPersistenceValue(browser, fromPersistenceRequest, value)
    }

    private fun getFromApplicationPersistence(browser: CefBrowser, fromPersistenceRequest: GetFromPersistenceRequest) {
        val value = JCEFApplicationPersistence.getInstance().get(PersistenceKey(fromPersistenceRequest.payload.key))
        sendPersistenceValue(browser, fromPersistenceRequest, value)
    }


    private fun sendPersistenceValue(browser: CefBrowser, fromPersistenceRequest: GetFromPersistenceRequest, value: String?) {

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
            ErrorReporter.getInstance().reportError("JCEFPersistenceService.sendPersistenceValue", e)
            val message = SetFromPersistenceMessage(
                SetFromPersistenceMessagePayload(
                    fromPersistenceRequest.payload.key,
                    null,
                    fromPersistenceRequest.payload.scope, ErrorPayload(e.toString())
                )
            )
            serializeAndExecuteWindowPostMessageJavaScript(browser, message)
        }
    }

    fun saveToPersistence(saveToPersistenceRequest: SaveToPersistenceRequest) {
        when (saveToPersistenceRequest.payload.scope) {
            Scope.application -> {
                JCEFProjectPersistence.getInstance(project).set(
                    PersistenceKey(saveToPersistenceRequest.payload.key),
                    saveToPersistenceRequest.payload.value
                )
            }

            Scope.project -> {
                JCEFApplicationPersistence.getInstance().set(
                    PersistenceKey(saveToPersistenceRequest.payload.key),
                    saveToPersistenceRequest.payload.value
                )

            }
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


    fun get(key: PersistenceKey): String? {
        return persistenceState.state[key.key]
    }

    fun set(key: PersistenceKey, value: String) {
        persistenceState.state[key.key] = value
    }

}

class JCEFPersistenceState {

    val state = mutableMapOf<String, String>()
}

class PersistenceKey(val key: String)
