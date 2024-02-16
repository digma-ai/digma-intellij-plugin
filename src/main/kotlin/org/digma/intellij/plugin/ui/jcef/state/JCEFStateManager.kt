package org.digma.intellij.plugin.ui.jcef.state

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class JCEFStateManager(private val project: Project) {

    private var state: JsonNode? = null

    companion object {
        fun getInstance(project: Project): JCEFStateManager {
            return project.service<JCEFStateManager>()
        }
    }


    fun updateState(state: JsonNode) {
        this.state = state
        fireStateChanged(state)
    }

    fun getState(): JsonNode? {
        return state
    }


    private fun fireStateChanged(state: JsonNode) {
        project.messageBus.syncPublisher(StateChangedEvent.JCEF_STATE_CHANGED_TOPIC)
            .stateChanged(state)
    }
}