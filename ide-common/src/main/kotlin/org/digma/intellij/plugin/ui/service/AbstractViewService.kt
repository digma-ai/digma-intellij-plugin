package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.Content
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel


abstract class AbstractViewService(val project: Project) {

    //these may be null if the tool window did not open yet
    var panel: DigmaTabPanel? = null
    var toolWindow: ToolWindow? = null
    var toolWindowContent: Content? = null

    private val toolWindowTabsHelper: ToolWindowTabsHelper = project.getService(ToolWindowTabsHelper::class.java)

    fun setVisible() {
        if (toolWindowTabsHelper.isErrorDetailsOn()){
            return
        }
        toolWindow?.contentManager?.setSelectedContent(toolWindowContent!!, false)
    }

    fun isVisible():Boolean{
        return toolWindow?.contentManager?.selectedContent === toolWindowContent
    }

    fun setContent(toolWindow: ToolWindow, content: Content) {
        this.toolWindow = toolWindow
        this.toolWindowContent = content
    }


    fun updateUi(){

        //don't update ui if error details is on,
        //when error details is closed the ui will be updated.
        //the models do get updated on every context changed

        if (toolWindowTabsHelper.isErrorDetailsOn()){
            return
        }

        if (toolWindowContent != null){
            toolWindowContent?.displayName = getViewDisplayName()
        }

        if (panel != null){
            panel?.reset()
        }

    }

    abstract fun getViewDisplayName(): String?

}