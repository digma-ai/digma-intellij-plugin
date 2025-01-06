package org.digma.intellij.plugin.analytics

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.startup.DigmaProjectActivity

class BackendInfoHolderStarter:DigmaProjectActivity() {
    override fun executeProjectStartup(project: Project) {
        //initialize BackendInfoHolder as early as possible so it will populate its info from the backend as soon as possible
        BackendInfoHolder.getInstance(project)
    }
}