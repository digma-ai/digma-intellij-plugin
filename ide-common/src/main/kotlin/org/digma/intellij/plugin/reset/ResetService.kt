package org.digma.intellij.plugin.reset

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.CoroutineScope
import org.digma.intellij.plugin.auth.AuthManager
import org.digma.intellij.plugin.kotlin.ext.launchWithErrorReporting
import org.digma.intellij.plugin.persistence.PersistenceService

@Service(Service.Level.APP)
class ResetService(val cs: CoroutineScope) {

    private val logger = Logger.getInstance(this::class.java)

    companion object {
        @JvmStatic
        fun getInstance(): ResetService {
            return service<ResetService>()
        }
    }


    fun resetUserId() {

        cs.launchWithErrorReporting("ResetService.resetUserId", logger) {
            PersistenceService.getInstance().nullifyUserId()
            PersistenceService.getInstance().nullifyUserRegistrationEmail()
            PersistenceService.getInstance().nullifyUserEmail()
            AuthManager.getInstance().logoutSynchronously("reset plugin")
            ApplicationManager.getApplication().invokeLater {
                ApplicationManager.getApplication().restart()
            }
        }
    }


}