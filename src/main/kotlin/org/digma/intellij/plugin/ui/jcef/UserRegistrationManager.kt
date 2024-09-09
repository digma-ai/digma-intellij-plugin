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


        //if UserRequestedCourse is already true don't change again, it depends on who called this method, and it may be that
        // the scope property doesn't exist but user already requested course before.
        if (!PersistenceService.getInstance().isUserRequestedCourse()) {
            val courseRequested = registrationMap["scope"] == "promotion"
            if (courseRequested) {
                PersistenceService.getInstance().setUserRequestedCourse()
                ActivityMonitor.getInstance(project).registerUserRequestedCourse()
            }
        }

        if (!PersistenceService.getInstance().isUserRequestedEarlyAccess()) {
            val earlyAccessRequested = registrationMap["scope"] == "early-access"
            if (earlyAccessRequested) {
                PersistenceService.getInstance().setUserRequestedEarlyAccess()
                ActivityMonitor.getInstance(project).registerUserRequestedEarlyAccess()
            }
        }

        if (PersistenceService.getInstance().getUserRegistrationEmail().isNullOrBlank() ||
            PersistenceService.getInstance().getUserRegistrationEmail() != email
        ) {

            ActivityMonitor.getInstance(project).registerCustomEvent("register local user", registrationMap)
            ActivityMonitor.getInstance(project).registerUserAction("local user registered", registrationMap)
            PersistenceService.getInstance().setUserRegistrationEmail(email)
            ActivityMonitor.getInstance(project).registerEmail(email)//override the onboarding email
            project.messageBus.syncPublisher(UserRegistrationEvent.USER_REGISTRATION_TOPIC).userRegistered(email)
        }
    }

}