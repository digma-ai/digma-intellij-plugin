package org.digma.intellij.plugin.analytics

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.model.rest.environment.Env

const val LOCAL_ENV = "LOCAL"
const val LOCAL_TESTS_ENV = "LOCAL-TESTS"


fun getAllEnvironments(project: Project): List<Env> {
    return AnalyticsService.getInstance(project).environment.environments
}

fun getAllEnvironmentsNames(project: Project): List<String> {
    return AnalyticsService.getInstance(project).environment.environments.map { it.name }
}

fun getCurrentEnvironment(project: Project): Env? {
    return AnalyticsService.getInstance(project).environment.current
}

fun getCurrentEnvironmentName(project: Project): String? {
    return AnalyticsService.getInstance(project).environment.current?.name
}

fun getCurrentEnvironmentId(project: Project): String? {
    return AnalyticsService.getInstance(project).environment.current?.id
}

fun getEnvironmentNameById(project: Project, envId: String): String? {
    return AnalyticsService.getInstance(project).environment.findById(envId)?.name
}

fun setCurrentEnvironmentById(project: Project, envId: String) {
    AnalyticsService.getInstance(project).environment.setCurrentById(envId)
}

fun setCurrentEnvironmentById(project: Project, envId: String, taskToRunAfterChange: Runnable) {
    AnalyticsService.getInstance(project).environment.setCurrentById(envId, taskToRunAfterChange)
}

fun refreshEnvironmentsNowOnBackground(project: Project) {
    AnalyticsService.getInstance(project).environment.refreshNowOnBackground()
}
