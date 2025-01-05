package org.digma.intellij.plugin.idea.execution

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.startup.DigmaProjectActivity

class OTELJarProviderStartup : DigmaProjectActivity() {

    override fun executeProjectStartup(project: Project) {
        //initialize the service on startup so that it will unpack the files as early as possible
        OTELJarProvider.getInstance()
    }
}