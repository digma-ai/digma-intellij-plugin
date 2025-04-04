package org.digma.intellij.plugin.common

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.IdeFocusManager

fun findActiveProject(): Project? {

    //best effort to find an active project

    var project = ProjectUtil.getActiveProject()
    if (project == null || !isProjectValid(project)) {
        project = IdeFocusManager.getGlobalInstance().lastFocusedFrame?.project
    }

    if (project == null || !isProjectValid(project)) {
        project = ProjectManager.getInstance().openProjects.firstOrNull { p -> !p.isDisposed }
    }

    return project?.takeIf { isProjectValid(project) }

}


fun isProjectValid(project: Project?): Boolean {
    return project != null && !project.isDisposed && !project.isDefault && project.isOpen
}