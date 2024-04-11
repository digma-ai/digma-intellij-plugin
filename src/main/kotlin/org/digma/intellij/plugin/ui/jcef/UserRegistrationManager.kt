package org.digma.intellij.plugin.ui.jcef

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.errorreporting.SEVERITY_HIGH_TRY_FIX
import org.digma.intellij.plugin.errorreporting.SEVERITY_PROP_NAME
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

    fun register(registrationMap: Map<String, String>) {

        ActivityMonitor.getInstance(project).registerCustomEvent("register user", registrationMap)
        ActivityMonitor.getInstance(project).registerUserAction("user registered", registrationMap)

        registrationMap["email"]?.let { userEmail ->
            PersistenceService.getInstance().setUserRegistrationEmail(userEmail)
            ActivityMonitor.getInstance(project).registerEmail(userEmail)//override the onboarding email
            project.messageBus.syncPublisher(UserRegistrationEvent.USER_REGISTRATION_TOPIC).userRegistered(userEmail)
        } ?: ErrorReporter.getInstance().reportError(
            project, "UserRegistrationManager.register", "register user email", mapOf(
                SEVERITY_PROP_NAME to SEVERITY_HIGH_TRY_FIX,
                "error" to "user registration request without email"
            )
        )
    }

}