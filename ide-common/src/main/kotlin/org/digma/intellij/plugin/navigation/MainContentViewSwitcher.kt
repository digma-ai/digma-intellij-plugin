package org.digma.intellij.plugin.navigation

import com.fasterxml.jackson.annotation.JsonFormat
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.insights.ErrorsViewOrchestrator
import org.digma.intellij.plugin.posthog.ActivityMonitor
import java.awt.CardLayout
import java.awt.Container

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class View(
    val title: String,
    val id: String,
) {

    Insights("Insights", "insights"),
    Assets("Assets", "assets"),
    Errors("Errors", "errors"),
    ErrorDetails("Error Details", "errors"),
    Tests("Tests", "tests");

    companion object {
        fun findById(id: String?): View? {
            return View.values().find { it.id == id }
        }
    }
}


@Service(Service.Level.PROJECT)
class MainContentViewSwitcher(val project: Project) {

    private lateinit var myLayout: CardLayout
    private lateinit var mainContentPanel: Container

    var currentView: View? = null

    companion object {
        @JvmStatic
        fun getInstance(project: Project): MainContentViewSwitcher {
            return project.service<MainContentViewSwitcher>()
        }
    }

    fun setLayout(myLayout: CardLayout, mainContentPanel: Container) {
        this.myLayout = myLayout
        this.mainContentPanel = mainContentPanel
    }


    fun showInsights() {
        showView(View.Insights)
    }

    fun showAssets() {
        showView(View.Assets)
    }

    fun showErrors() {
        showView(View.Errors)
    }

    fun showErrorDetails() {
        showView(View.ErrorDetails)
    }

    fun showTests() {
        showView(View.Tests)
    }


    fun showView(view: View) {
        showView(view, true)
    }

    fun showView(view: View, fireEvent: Boolean) {

        if (view == currentView) {
            return
        }

        if (view != View.ErrorDetails) {
            project.service<ErrorsViewOrchestrator>().closeErrorDetails()
        }

        if (view == View.Insights && currentView != View.Insights) {
            ActivityMonitor.getInstance(project).clearLastInsightsViewed()
        }



        currentView = view
        myLayout.show(mainContentPanel, view.id)
        currentView?.takeIf { fireEvent }?.let {
            fireViewChanged(it)
        }
    }


    private fun fireViewChanged(view: View) {
        val publisher = project.messageBus.syncPublisher(ViewChangedEvent.VIEW_CHANGED_TOPIC)
        publisher.viewChanged(view)

    }
}