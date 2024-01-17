package org.digma.intellij.plugin.ui.jcef

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.jcef.common.UserRegistrationEvent
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor

@Service(Service.Level.PROJECT)
class RegistrationEventHandler(private val project: Project) {
    companion object {
        @JvmStatic
        fun getInstance(project: Project): RegistrationEventHandler {
            return project.getService(RegistrationEventHandler::class.java)
        }
    }

    fun register(jsonNode: JsonNode) {
        val payloadNode = jsonNode.get("payload")
        val map: Map<String, String> =
            payloadNode.fields().asSequence()
                .associate { mutableEntry: MutableMap.MutableEntry<String, JsonNode> -> Pair(mutableEntry.key, mutableEntry.value.asText()) }

        project.service<ActivityMonitor>().registerUserActionEvent("register user", map)

        val email = map["email"].toString()
        project.service<ActivityMonitor>().registerEmail(email)//override the onboarding email
        PersistenceService.getInstance().setUserRegistrationEmail(email)

        val publisher: UserRegistrationEvent = project.messageBus
            .syncPublisher(UserRegistrationEvent.USER_REGISTRATION_TOPIC)
        publisher.userRegistered(email)
    }

}