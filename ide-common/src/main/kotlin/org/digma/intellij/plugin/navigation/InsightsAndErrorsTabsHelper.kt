package org.digma.intellij.plugin.navigation

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import javax.swing.JTabbedPane

/**
 * this is a helper for the insights and errors tab, its main job is to remember if error details is
 * on so the view doesn't change when moving in the editor. and to switch between insights and errors tab.
 * it is initialized lazy when the [org.digma.intellij.plugin.ui.common.InsightsPanel] is initialized.
 */
@Service(Service.Level.PROJECT)
class InsightsAndErrorsTabsHelper(val project: Project) {

    private var tabIndexBeforeErrorDetails: Int? = null
    private var errorDetailsOn = false

    //the insights and errors tabs
    private var insightsAndErrorsTabbedPane:JTabbedPane? = null
    private var insightsTabIndex: Int? = null
    private var errorsTabIndex: Int? = null
    private var errorsTabName:String? = null
    private var errorsDetailsTabName:String? = null



    fun setTabbedPane(tabbedPane: JTabbedPane, errorsTabName: String, errorsDetailsTabName: String) {
        this.insightsAndErrorsTabbedPane = tabbedPane
        this.errorsTabName = errorsTabName
        this.errorsDetailsTabName = errorsDetailsTabName
    }



    fun rememberCurrentTab() {
        tabIndexBeforeErrorDetails = insightsAndErrorsTabbedPane?.selectedIndex
    }


    fun setInsightsTabIndex(index: Int) {
        insightsTabIndex = index
    }

    fun setErrorsTabIndex(index: Int) {
        errorsTabIndex = index
    }


    fun errorDetailsClosed(switchToPreviousTab:Boolean = true) {
        if (switchToPreviousTab) {
            tabIndexBeforeErrorDetails?.let {
                insightsAndErrorsTabbedPane?.selectedIndex = it
            }
        }
        tabIndexBeforeErrorDetails = null
    }


    fun errorDetailsOn() {
        errorDetailsOn = true
        errorsTabIndex?.let {
            insightsAndErrorsTabbedPane?.setTitleAt(it, errorsDetailsTabName)
        }

    }

    fun errorDetailsOff() {
        errorDetailsOn = false
        errorsTabIndex?.let {
            insightsAndErrorsTabbedPane?.setTitleAt(it, errorsTabName)
        }
    }

    fun errorDetailsOffNoTitleChange() {
        errorDetailsOn = false
    }

    fun isErrorDetailsOn(): Boolean {
        return errorDetailsOn
    }



    fun switchToErrorsTab() {
        errorsTabIndex?.let {
            insightsAndErrorsTabbedPane?.selectedIndex = it
        }
    }

    fun switchToInsightsTab() {
        insightsTabIndex?.let {
            insightsAndErrorsTabbedPane?.selectedIndex = it
        }
    }

}