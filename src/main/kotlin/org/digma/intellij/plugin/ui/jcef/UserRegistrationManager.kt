package org.digma.intellij.plugin.ui.jcef

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor

@Service(Service.Level.PROJECT)
class UserRegistrationManager(private val project: Project) {
    companion object {
        @JvmStatic
        fun getInstance(project: Project): UserRegistrationManager {
            return project.getService(UserRegistrationManager::class.java)
        }
    }

    fun register(registrationMap: Map<String, String>, trigger: String) {

        val email = registrationMap["email"]

        if (email.isNullOrBlank()) {
            ErrorReporter.getInstance().reportError(
                project, "UserRegistrationManager.register", "register user email", mapOf(
                    "error" to "user registration request without email",
                    "registration trigger" to trigger
                )
            )
            return
        }


        if (PersistenceService.getInstance().getUserRegistrationEmail().isNullOrBlank() ||
            PersistenceService.getInstance().getUserRegistrationEmail() != email
        ) {

            ActivityMonitor.getInstance(project).registerCustomEvent("register local user", registrationMap)
            ActivityMonitor.getInstance(project).registerUserAction("local user registered", registrationMap)
            val courseRequested = registrationMap["scope"] == "promotion"
            PersistenceService.getInstance().setUserRegistrationEmail(email)
            ActivityMonitor.getInstance(project).registerEmail(email, courseRequested)//override the onboarding email
            project.messageBus.syncPublisher(UserRegistrationEvent.USER_REGISTRATION_TOPIC).userRegistered(email)
        }
    }

}