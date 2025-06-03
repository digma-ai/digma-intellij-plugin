package org.digma.intellij.plugin.analytics

import com.intellij.openapi.project.Project
import org.apache.maven.artifact.versioning.ComparableVersion
import org.digma.intellij.plugin.auth.account.DigmaDefaultAccountHolder
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.model.rest.UNKNOWN_APPLICATION_VERSION
import org.digma.intellij.plugin.model.rest.version.BackendDeploymentType

fun isCentralized(project: Project): Boolean {
    return BackendInfoHolder.getInstance(project).isCentralized()
}

fun getBackendVersion(project: Project): String {
    return BackendInfoHolder.getInstance(project).getAbout().applicationVersion
}

fun getBackendVersion(): String {
    val project = findActiveProject()
    return project?.let {
        BackendInfoHolder.getInstance(it).getAbout().applicationVersion
    } ?: UNKNOWN_APPLICATION_VERSION
}

fun getBackendDeploymentType(): String {
    val project = findActiveProject()
    return project?.let {
        BackendInfoHolder.getInstance(it).getAbout().deploymentType?.name ?: BackendDeploymentType.Unknown.name
    } ?: BackendDeploymentType.Unknown.name
}

fun isCentralized(): Boolean {
    val project = findActiveProject()
    return project?.let {
        BackendInfoHolder.getInstance(it).isCentralized()
    } ?: false
}


fun isNoAccountInCentralized(): Boolean {
    return isCentralized() && DigmaDefaultAccountHolder.getInstance().account == null
}

fun backendVersionOlderThen(project: Project, version: String): Boolean {
    val backendVersion = BackendInfoHolder.getInstance(project).getAbout().applicationVersion

    //dev environment may return unknown
    if (UNKNOWN_APPLICATION_VERSION.equals(backendVersion, true)) {
        return false;
    }

    val currentBackendVersion = ComparableVersion(backendVersion);
    val requiredBackendVersion = ComparableVersion(version);

    return currentBackendVersion < requiredBackendVersion;
}