package org.digma.intellij.plugin.posthog

import com.intellij.openapi.project.Project

class SimpleEventInterceptor(private val project: Project) {

    //simple solution to intercept certain events and do something more with them

    fun intercept(eventName: String, details: Map<String, Any>): Boolean {

        //with catch 'jira ticket field copy button clicked' and ''tests jira ticket field copy button clicked
        if (eventName.trim().contains("Jira ticket field copy button clicked", true)) {
            registerJiraFieldCopiedEvent(eventName, details)
            return true
        }

        return false
    }


    private fun registerJiraFieldCopiedEvent(eventName: String, details: Map<String, Any>) {
        ActivityMonitor.getInstance(project).registerJiraFieldCopied(eventName, details)
    }


}