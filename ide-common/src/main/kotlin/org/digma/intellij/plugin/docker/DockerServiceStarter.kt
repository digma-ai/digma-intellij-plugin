package org.digma.intellij.plugin.docker

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.startup.DigmaProjectActivity

class DockerServiceStarter: DigmaProjectActivity() {
    //initialize DockerService as early as possible so it will download docker compose file early
    override fun executeProjectStartup(project: Project) {
        DockerService.getInstance()
    }
}