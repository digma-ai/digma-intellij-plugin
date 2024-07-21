package org.digma.intellij.plugin.auth

import kotlinx.coroutines.runBlocking
import org.digma.intellij.plugin.auth.account.DigmaAccount
import org.digma.intellij.plugin.auth.account.DigmaAccountManager
import org.digma.intellij.plugin.auth.account.DigmaDefaultAccountHolder
import org.digma.intellij.plugin.auth.credentials.DigmaCredentials
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.scheduling.oneShotTask
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds


fun reportPosthogEvent(evenName: String, details: Map<String, String> = mapOf()) {
    findActiveProject()?.let { project ->
        ActivityMonitor.getInstance(project).registerCustomEvent(evenName, details)
    }
}


fun updateAccount(digmaAccount: DigmaAccount, digmaCredentials: DigmaCredentials) {

    //this is the only place we update the account and credentials.
    val future = oneShotTask("AuthManager.updateAccount") {
        runBlocking {
            DigmaAccountManager.getInstance().updateAccount(digmaAccount, digmaCredentials)
            DigmaDefaultAccountHolder.getInstance().account = digmaAccount
            CredentialsHolder.digmaCredentials = digmaCredentials
        }
    }

    try {
        if (future == null) {
            ErrorReporter.getInstance().reportError("AuthManager.updateAccount", "could not execute updateAccount task", mapOf())
            throw RuntimeException("could not execute updateAccount task")
        } else {
            future.get(2.seconds.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        }
    } catch (e: Throwable) {
        ErrorReporter.getInstance().reportError("AuthManager.updateAccount", e)
        throw e
    }
}


fun deleteAccount(account: DigmaAccount) {
    //this is the only place we delete the account.
    val future = oneShotTask("AuthManager.deleteAccount") {
        runBlocking {
            DigmaAccountManager.getInstance().removeAccount(account)
            DigmaDefaultAccountHolder.getInstance().account = null
            CredentialsHolder.digmaCredentials = null
        }
    }

    try {
        if (future == null) {
            ErrorReporter.getInstance().reportError("AuthManager.deleteAccount", "could not execute deleteAccount account task", mapOf())
            throw RuntimeException("could not execute deleteAccount task")
        } else {
            future.get(2.seconds.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        }
    } catch (e: Throwable) {
        ErrorReporter.getInstance().reportError("AuthManager.updateAccount", e)
        throw e
    }

}


