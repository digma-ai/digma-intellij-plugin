package org.digma.intellij.plugin.reset

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.auth.AuthManager
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.persistence.PersistenceService

@Service(Service.Level.APP)
class ResetService(val cs: CoroutineScope) {

    companion object {
        @JvmStatic
        fun getInstance(): ResetService {
            return service<ResetService>()
        }
    }


    fun resetUserId() {

        cs.launch {
            try {
                PersistenceService.getInstance().nullifyUserId()
                PersistenceService.getInstance().nullifyUserRegistrationEmail()
                PersistenceService.getInstance().nullifyUserEmail()
                AuthManager.getInstance().logoutSynchronously()
                ApplicationManager.getApplication().invokeLater {
                    ApplicationManager.getApplication().restart()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                ErrorReporter.getInstance().reportError("ResetService.resetUserId", e)
            }
        }
    }


}