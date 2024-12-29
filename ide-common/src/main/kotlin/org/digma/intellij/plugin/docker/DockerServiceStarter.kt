package org.digma.intellij.plugin.docker

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.startup.DigmaProjectActivity

class DockerServiceStarter : DigmaProjectActivity() {

    override fun executeProjectStartup(project: Project) {
        //initialize the docker service as early as possible
        DockerService.getInstance()
    }
}