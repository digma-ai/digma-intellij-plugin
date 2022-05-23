package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.Content
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret
import org.digma.intellij.plugin.model.rest.summary.MethodCodeObjectSummary
import org.digma.intellij.plugin.ui.model.errors.ErrorsModel
import org.digma.intellij.plugin.ui.panels.ResettablePanel

class ErrorsViewService(val project: Project) {

    lateinit var panel: ResettablePanel
    lateinit var model: ErrorsModel
    lateinit var toolWindow: ToolWindow
    lateinit var errorsContent: Content

    fun updateSelectedMethod(
        methodUnderCaret: MethodUnderCaret,
        methodCodeObjectSummary: MethodCodeObjectSummary?
    ) {
        model.methodName = methodUnderCaret.name
        model.errorsCount = 0
        methodCodeObjectSummary?.let { it ->
            it.insightsCount.also {
                model.errorsCount = it
            }
        }
        panel.reset()
    }

    fun empty() {

    }


    fun setVisible(visible: Boolean) {
        toolWindow.contentManager.setSelectedContent(errorsContent, true)
    }

    fun setContent(toolWindow: ToolWindow, errorsContent: Content) {
        this.toolWindow = toolWindow
        this.errorsContent = errorsContent
    }


}