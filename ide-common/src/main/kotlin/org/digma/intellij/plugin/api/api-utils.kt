package org.digma.intellij.plugin.api

import com.intellij.ide.RecentProjectListActionProvider
import com.intellij.ide.RecentProjectsManager
import com.intellij.ide.ReopenProjectAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import org.digma.intellij.plugin.common.findActiveProject

const val ACTION_PARAM_NAME = "action"
const val TARGET_TAB_PARAM_NAME = "targetTab"
const val SPAN_UID_PARAM_NAME = "spanUid"
const val ENVIRONMENT_ID_PARAM_NAME = "environment_id"
const val PROJECT_NAME_PARAM_NAME = "projectName"

const val ACTION_GO_TO_HOME_PARAM_VALUE = "GoToHome"
const val ACTION_GO_TO_SPAN_PARAM_VALUE = "GoToSpan"
const val ACTION_OPEN_REPORT_PARAM_VALUE = "OpenReport"


fun findProjectOrNull(projectName: String): Project? {

    var project = ProjectManager.getInstance().openProjects.find { it.name == projectName }
    if (project == null) {
        val projectPath =
            RecentProjectListActionProvider.getInstance().getActions().asSequence().filterIsInstance(ReopenProjectAction::class.java).firstOrNull {
                it.projectName == projectName
            }?.projectPath

        if (projectPath != null) {
            project = ProjectManager.getInstance().loadAndOpenProject(projectPath)
        }
    }

    return project
}


fun findActiveOrRecentProject(): Project? {
    return findActiveProject() ?: findRecentProject()
}

fun findRecentProject(): Project? {
    val recentProjectPath =
        RecentProjectListActionProvider.getInstance().getActions().asSequence().filterIsInstance(ReopenProjectAction::class.java)
            .firstOrNull()?.projectPath ?: RecentProjectsManager.getInstance().lastProjectCreationLocation

    return recentProjectPath?.let {
        ProjectManager.getInstance().loadAndOpenProject(recentProjectPath)
    }
}
