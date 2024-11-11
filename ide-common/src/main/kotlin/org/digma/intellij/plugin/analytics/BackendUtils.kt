package org.digma.intellij.plugin.analytics

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.common.findActiveProject

fun isCentralized(project: Project): Boolean {
    return BackendInfoHolder.getInstance(project).isCentralized()
}

fun getBackendVersion(project: Project): String {
    return BackendInfoHolder.getInstance(project).getAbout()?.applicationVersion ?: "unknown"
}

fun getBackendVersion(): String {
    val project = findActiveProject()
    return project?.let {
        BackendInfoHolder.getInstance(it).getAbout()?.applicationVersion ?: "unknown"
    } ?: "unknown"
}

fun getBackendDeploymentType(): String {
    val project = findActiveProject()
    return project?.let {
        BackendInfoHolder.getInstance(it).getAbout()?.deploymentType?.name ?: "unknown"
    } ?: "unknown"
}

fun isCentralized(): Boolean {
    val project = findActiveProject()
    return project?.let {
        BackendInfoHolder.getInstance(it).getAbout()?.isCentralize ?: false
    } ?:false
}
