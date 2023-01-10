package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.project.Project
class CodeAnalyticsViewService(project: Project) : AbstractViewService(project) {
    override fun getViewDisplayName(): String {
        return "Code Analytics"
    }

}