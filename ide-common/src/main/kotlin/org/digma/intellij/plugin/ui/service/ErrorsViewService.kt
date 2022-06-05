package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.Content
import org.digma.intellij.plugin.errors.ErrorsProvider
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.ui.model.EmptyScope
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.model.errors.ErrorsModel
import org.digma.intellij.plugin.ui.panels.ResettablePanel
import java.util.*

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
        model.scope = MethodScope(methodInfo)

        panel.reset()
    }


    fun contextChangeNoMethodInfo(dummy: MethodInfo) {
        model.listViewItems = ArrayList()
        model.scope = MethodScope(dummy)

        panel.reset()

    }


    fun empty() {
        model.listViewItems = Collections.emptyList()
        model.scope = EmptyScope("")

        panel.reset()
    }

    fun setVisible() {
        toolWindow.contentManager.setSelectedContent(errorsContent, true)
    }

    fun setContent(toolWindow: ToolWindow, errorsContent: Content) {
        this.toolWindow = toolWindow
        this.errorsContent = errorsContent
    }

}