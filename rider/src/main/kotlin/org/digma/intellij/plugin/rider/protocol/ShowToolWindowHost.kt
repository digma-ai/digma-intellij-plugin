package org.digma.intellij.plugin.rider.protocol

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.rd.platform.util.lifetime
import com.jetbrains.rdclient.util.idea.LifetimedProjectComponent
import com.jetbrains.rider.projectView.solution
import org.digma.intellij.plugin.navigation.MainContentViewSwitcher
import org.digma.intellij.plugin.ui.ToolWindowShower

class ShowToolWindowHost(project: Project) : LifetimedProjectComponent(project) {

    private val model: ShowToolWindowModel = project.solution.showToolWindowModel


    init {
        model.showToolWindow.advise(project.lifetime){
            project.service<MainContentViewSwitcher>().showInsights()
            ToolWindowShower.getInstance(project).showToolWindow()
        }
    }
}