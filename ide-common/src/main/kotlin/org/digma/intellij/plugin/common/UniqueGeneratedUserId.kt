package org.digma.intellij.plugin.common

import com.intellij.openapi.components.service
import org.digma.intellij.plugin.persistence.PersistenceService

object UniqueGeneratedUserId {

    val userId: String
    val isDevUser: Boolean

    init {
        val hostname = CommonUtils.getLocalHostname()
        if (System.getenv("devenv") == "digma") {
            userId = hostname
            isDevUser = true
        } else {
            if (service<PersistenceService>().getUserId() == null) {
                service<PersistenceService>().setUserId(Integer.toHexString(hostname.hashCode()))
            }
            userId = service<PersistenceService>().getUserId()!!
            isDevUser = false
        }
    }
}