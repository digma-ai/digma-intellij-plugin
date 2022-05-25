package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.Content
import org.digma.intellij.plugin.errors.ErrorsListContainer
import org.digma.intellij.plugin.errors.ErrorsProvider
import org.digma.intellij.plugin.insights.InsightsListContainer
import org.digma.intellij.plugin.insights.InsightsProvider
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret
import org.digma.intellij.plugin.model.rest.summary.MethodCodeObjectSummary
import org.digma.intellij.plugin.ui.model.errors.ErrorsModel
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import org.digma.intellij.plugin.ui.panels.ResettablePanel

class ErrorsViewService(val project: Project) {

    lateinit var panel: ResettablePanel
    lateinit var model: ErrorsModel
    lateinit var toolWindow: ToolWindow
    lateinit var errorsContent: Content
    private val errorsProvider: ErrorsProvider = project.getService(ErrorsProvider::class.java)


    fun contextChanged(
        methodInfo: MethodInfo
    ) {


        val errorsListContainer = errorsProvider.getErrors(methodInfo)
        model.listViewItems = errorsListContainer.listViewItems
        model.methodName = methodInfo.name
        model.className = methodInfo.containingClass
        model.errorsCount = errorsListContainer.count

        panel.reset()
    }

    fun empty() {
        model.listViewItems = ArrayList()
        model.methodName = ""
        model.className = ""
        model.errorsCount = 0

        panel.reset()

    }




    fun setVisible(visible: Boolean) {
        toolWindow.contentManager.setSelectedContent(errorsContent, true)
    }

    fun setContent(toolWindow: ToolWindow, errorsContent: Content) {
        this.toolWindow = toolWindow
        this.errorsContent = errorsContent
    }


}