package org.digma.intellij.plugin.auth

import kotlinx.coroutines.runBlocking
import org.digma.intellij.plugin.auth.account.DigmaAccount
import org.digma.intellij.plugin.auth.account.DigmaAccountManager
import org.digma.intellij.plugin.auth.account.DigmaDefaultAccountHolder
import org.digma.intellij.plugin.auth.credentials.DigmaCredentials
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.posthog.ActivityMonitor


fun reportPosthogEvent(evenName: String, details: Map<String, String> = mapOf()) {
    findActiveProject()?.let { project ->
        ActivityMonitor.getInstance(project).registerCustomEvent(evenName, details)
    }
}


fun updateAccount(digmaAccount: DigmaAccount, digmaCredentials: DigmaCredentials) {
    runBlocking {
        DigmaAccountManager.getInstance().updateAccount(digmaAccount, digmaCredentials)
        DigmaDefaultAccountHolder.getInstance().account = digmaAccount
    }
}