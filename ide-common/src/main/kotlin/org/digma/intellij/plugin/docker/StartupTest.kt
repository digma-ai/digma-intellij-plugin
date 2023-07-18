package org.digma.intellij.plugin.docker

import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity


class StartupTest : StartupActivity, DumbAware {
    override fun runActivity(project: Project) {

        if (project.service<DockerService>().isDockerInstalled()) {

//            project.service<DockerService>().installEngine()

//            service<DockerService>().stopEngine()
//            service<DockerService>().startEngine()

            service<DockerService>().removeEngine()


        }
    }
}