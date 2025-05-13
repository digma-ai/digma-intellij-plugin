package org.digma.intellij.plugin.rider.protocol

import com.intellij.openapi.project.Project
import com.jetbrains.rdclient.util.idea.LifetimedProjectComponent
import com.jetbrains.rider.projectView.solution
import org.digma.intellij.plugin.ui.ToolWindowShower

@Suppress("LightServiceMigrationCode")
class ShowToolWindowHost(project: Project) : LifetimedProjectComponent(project) {

    private val model: ShowToolWindowModel = project.solution.showToolWindowModel


    init {
        model.showToolWindow.advise(componentLifetime){
            ToolWindowShower.getInstance(project).showToolWindow()
        }
    }
}