package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.Content
import org.digma.intellij.plugin.analytics.AnalyticsServiceConnectionEvent
import org.digma.intellij.plugin.ui.model.NOT_SUPPORTED_OBJECT_MSG
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import javax.swing.SwingUtilities


abstract class AbstractViewService(val project: Project) : Disposable {

    //these may be null if the tool window did not open yet
    var panel: DigmaTabPanel? = null
    var toolWindow: ToolWindow? = null
    var toolWindowContent: Content? = null

    private val toolWindowTabsHelper: ToolWindowTabsHelper = project.getService(ToolWindowTabsHelper::class.java)

    init {
        //subscribe to connection lost/gained , call doUpdateUi() on each event so that the no connection card will show or hide
        project.messageBus.connect(this)
            .subscribe(AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC,
                handler = object : AnalyticsServiceConnectionEvent {
                    override fun connectionLost() {
                        doConnectionLost()
                        doUpdateUi()
                    }

                    override fun connectionGained() {
                        doConnectionGained()
                        doUpdateUi()
                    }
                }
            )
    }


    fun doConnectionLost() {
        //if a view needs to do something when connection lost can override this method and don't forget to call super
        if (toolWindowTabsHelper.isErrorDetailsOn()) {
            toolWindowTabsHelper.errorDetailsOff()
            if (this is ErrorsViewService) {
                this.closeErrorDetails()
            }
            toolWindowTabsHelper.errorDetailsClosed()
        }
    }

    fun doConnectionGained() {
        //if a view needs to do something when connection gained can override this method and don't forget to call super
    }

    abstract fun getViewDisplayName(): String

    //in some situation the UI should not be updated, for example if the error details is On then nothing changes
    //in the view until its closed. there may be exceptions, for example the summary view can reload while error details
    // is on but setVisible should not run.
    open fun canUpdateUI(): Boolean {
        return !toolWindowTabsHelper.isErrorDetailsOn()
    }

    open fun canSetVisible(): Boolean {
        return !toolWindowTabsHelper.isErrorDetailsOn()
    }


    fun setVisible() {
        if (!canSetVisible()) {
            return
        }

        val r = Runnable {
            toolWindow?.contentManager?.setSelectedContent(toolWindowContent!!, false)
            toolWindowContent?.component?.revalidate()
            panel?.reset()
        }

        if (SwingUtilities.isEventDispatchThread()) {
            r.run()
        } else {
            SwingUtilities.invokeLater {
                r.run()
            }
        }
    }

    @Suppress("unused")
    fun isVisible():Boolean{
        return toolWindow?.contentManager?.selectedContent === toolWindowContent
    }

    fun setContent(toolWindow: ToolWindow, content: Content) {
        this.toolWindow = toolWindow
        this.toolWindowContent = content
    }


    fun updateUi() {

        //don't update ui if error details is on,
        //when error details is closed the ui will be updated.
        //the models do get updated on every context changed

        if (!canUpdateUI()) {
            return
        }

        doUpdateUi()
    }


    //this method should never be called directly,only when connection is lost or gained.
    //it by-pass the conditions usually tested in updateUi so that the no-connection card will show on all tabs.
    private fun doUpdateUi() {

        if (panel != null) {

            val r = Runnable {
                panel?.reset()

                if (toolWindowContent != null) {
                    toolWindowContent?.displayName = getViewDisplayName()
                    //reset focusable component methods, some panels are dynamic in what they return, see for example NoConnectionWrapper
                    toolWindowContent?.setPreferredFocusedComponent { panel?.getPreferredFocusedComponent() }
                    toolWindowContent?.preferredFocusableComponent = panel?.getPreferredFocusableComponent()
                    toolWindowContent?.component?.revalidate()
                    toolWindow?.component?.revalidate()
                }
            }

            if (SwingUtilities.isEventDispatchThread()) {
                r.run()
            } else {
                SwingUtilities.invokeLater {
                    r.run()
                }
            }
        }
    }

    protected fun getNonSupportedFileScopeMessage(fileUri: String?): String {
        return NOT_SUPPORTED_OBJECT_MSG + " " + fileUri?.substringAfterLast('/', fileUri)
    }

    override fun dispose() {
        //do nothing
    }
}