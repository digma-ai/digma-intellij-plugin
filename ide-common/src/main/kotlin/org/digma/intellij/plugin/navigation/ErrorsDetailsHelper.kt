package org.digma.intellij.plugin.navigation

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

/**
 * this is a helper for the insights and errors tab, its main job is to remember if error details is
 * on so the view doesn't change when moving in the editor. and to switch between insights and errors tab.
 * it is initialized lazy when the [org.digma.intellij.plugin.ui.common.InsightsPanel] is initialized.
 */
@Service(Service.Level.PROJECT)
class ErrorsDetailsHelper(val project: Project) {

    //todo: move the login to MainContentViewSwitcher

    private var viewBeforeErrorDetails: View? = null
    private var errorDetailsOn = false


    fun markCurrentView() {
        viewBeforeErrorDetails = MainContentViewSwitcher.getInstance(project).currentView
    }


    fun errorDetailsClosed(switchToPreviousTab:Boolean = true) {
        if (switchToPreviousTab) {
            viewBeforeErrorDetails?.let {
                MainContentViewSwitcher.getInstance(project).showView(it)
            }

        }
        viewBeforeErrorDetails = null
    }


    fun errorDetailsOn() {
        errorDetailsOn = true

    }

    fun errorDetailsOff() {
        errorDetailsOn = false
    }


    fun isErrorDetailsOn(): Boolean {
        return errorDetailsOn
    }
}