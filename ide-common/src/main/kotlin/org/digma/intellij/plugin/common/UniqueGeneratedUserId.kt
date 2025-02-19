package org.digma.intellij.plugin.common

import com.intellij.openapi.components.service
import org.apache.commons.codec.digest.DigestUtils
import org.digma.intellij.plugin.persistence.PersistenceService
import java.net.NetworkInterface
import java.util.UUID

object UniqueGeneratedUserId {

    val userId: String
    val isDevUser: Boolean

    init {
        if (System.getenv("devenv") == "digma") {
            val userName = System.getProperty("user.name") ?: "user"
            userId = "$userName@${CommonUtils.getLocalHostname()}"
            isDevUser = true
        } else if (System.getenv("devenv") == "externalUserSimulator") {
            //this is just to overcome my own deleted external user which can not be recreated in posthog
            if (service<PersistenceService>().getUserId() == null) {
                service<PersistenceService>().setUserId(generateUniqueUserId("digma"))
            }
            userId = service<PersistenceService>().getUserId()!!
            isDevUser = false
        } else {
            if (service<PersistenceService>().getUserId() == null) {
                service<PersistenceService>().setUserId(generateUniqueUserId())
            }
            userId = service<PersistenceService>().getUserId()!!
            isDevUser = false
        }
    }
}


//this method is outside the class so that it can be tested,UniqueGeneratedUserId can not be instantiated in regular unit tests
fun generateUniqueUserId(salt: String = ""): String {
    try {
        val userName = System.getProperty("user.name")
        val userHome = System.getProperty("user.home")
        val osName = System.getProperty("os.name")
        val osArch = System.getProperty("os.arch")
        //MUST BE SORTED
        val ni = NetworkInterface.networkInterfaces().toList().mapNotNull { it.hardwareAddress }.map { macAddressToString(it) }.sorted()
            .joinToString("-")
        val baseString = salt + userName + userHome + osName + osArch + ni
        return DigestUtils.sha1Hex(baseString)
    } catch (e: Throwable) {
        return DigestUtils.sha1Hex(UUID.randomUUID().toString())
    }
}


private fun macAddressToString(hardwareAddress: ByteArray?): String {

    if (hardwareAddress == null) {
        return ""
    }

    val sb = StringBuilder()
    for (i in hardwareAddress.indices) {
        sb.append(java.lang.String.format("%02X%s", hardwareAddress[i], if ((i < hardwareAddress.size - 1)) "-" else ""))
    }
    return sb.toString()
}