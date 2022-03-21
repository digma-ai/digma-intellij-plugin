package com.github.shalom938.digmajetbrainsplugin.services

import com.intellij.openapi.project.Project
import com.github.shalom938.digmajetbrainsplugin.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
