package org.digma.intellij.plugin.common

import com.intellij.openapi.components.service
import org.digma.intellij.plugin.persistence.PersistenceService

object UserId {

    val userId: String
    val isDevUser: Boolean

    init {
        val hostname = CommonUtils.getLocalHostname()
        if (System.getenv("devenv") == "digma") {
            userId = hostname
            isDevUser = true
        } else {
            if (service<PersistenceService>().state.userId == null) {
                service<PersistenceService>().state.userId = Integer.toHexString(hostname.hashCode())
            }
            userId = service<PersistenceService>().state.userId!!
            isDevUser = false
        }
    }


}