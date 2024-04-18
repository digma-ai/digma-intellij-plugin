package org.digma.intellij.plugin.analytics

import com.intellij.openapi.project.Project


fun isCentralized(project: Project): Boolean {
    return BackendInfoHolder.getInstance().isCentralized(project)
}

fun isCentralized(): Boolean {
    return BackendInfoHolder.getInstance().isCentralized()
}