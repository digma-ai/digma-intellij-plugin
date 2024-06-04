package org.digma.intellij.plugin.analytics

import com.intellij.openapi.project.Project

fun isCentralized(project: Project): Boolean {
    return BackendInfoHolder.getInstance(project).isCentralized()
}

fun getVersion(project: Project): String {
    return BackendInfoHolder.getInstance(project).getAbout()?.applicationVersion ?: "unknown";
}