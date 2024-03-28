package org.digma.intellij.plugin.analytics

import com.intellij.openapi.project.Project


fun isCentralized(project: Project): Boolean {

    return try {
        val about = AnalyticsService.getInstance(project).about
        about.isCentralize ?: false
    } catch (e: AnalyticsServiceException) {
        false
    }

}