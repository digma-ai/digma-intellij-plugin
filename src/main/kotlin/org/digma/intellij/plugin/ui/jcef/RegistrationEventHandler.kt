package org.digma.intellij.plugin.ui.jcef

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor

@Service(Service.Level.PROJECT)
class RegistrationEventHandler(private val project: Project) : Disposable {
    companion object {
        @JvmStatic
        fun getInstance(project: Project): RegistrationEventHandler {
            return project.getService(RegistrationEventHandler::class.java)
        }
    }

    fun register(jsonNode: JsonNode, browser: CefBrowser) {
        val payloadNode = jsonNode.get("payload")
        val map: Map<String, String> = payloadNode.fields().asSequence().associate {
            Pair(it.key, it.value.toString())
        }

        project.service<ActivityMonitor>().registerUserActionEvent("register user",
            map)

        val email = map["email"].toString()
        PersistenceService.getInstance().state.userEmail = email
        sendUserEmail(browser, email)
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }
}