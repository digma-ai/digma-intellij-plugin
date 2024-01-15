package org.digma.intellij.plugin.common

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.IdeFocusManager
import java.awt.Window

fun findActiveProject(): Project? {

    //best effort to find a project for the notification

    var project = ProjectUtil.getActiveProject()
    if (project == null || project.isDisposed) {
        project = IdeFocusManager.getGlobalInstance().lastFocusedFrame?.project
    }

    if (project == null || project.isDisposed) {
        project = ProjectManager.getInstance().openProjects.firstOrNull { p -> !p.isDisposed }
    }

    return project

}