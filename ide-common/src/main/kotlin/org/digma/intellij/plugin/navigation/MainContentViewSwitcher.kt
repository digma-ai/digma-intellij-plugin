package org.digma.intellij.plugin.navigation

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.base.Objects
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.navigation.View.Companion.getSelected
import org.digma.intellij.plugin.navigation.View.Companion.hideErrorDetails
import org.digma.intellij.plugin.navigation.View.Companion.hideErrors
import org.digma.intellij.plugin.navigation.View.Companion.setSelected
import org.digma.intellij.plugin.navigation.View.Companion.views
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.ui.service.ErrorsViewOrchestrator
import java.awt.CardLayout
import java.awt.Container

data class View
private constructor(
    val title: String,
    val id: String,
    val cardName: String,
    @get:JsonProperty("isDisabled")
    @param:JsonProperty("isDisabled")
    var isDisabled: Boolean = false,
    @get:JsonProperty("isSelected")
    @param:JsonProperty("isSelected")
    var isSelected: Boolean = false,
    @get:JsonProperty("hasNewData")
    @param:JsonProperty("hasNewData")
    var hasNewData: Boolean = false,
    @get:JsonProperty("isHidden")
    @param:JsonProperty("isHidden")
    var isHidden: Boolean = false,
) {

    override fun equals(other: Any?): Boolean {
        return other is View && other.id == id && other.title == title && other.cardName == cardName
    }

    override fun hashCode(): Int {
        return Objects.hashCode(id, title)
    }

    companion object {

        val Insights = View(title = "Insights", id = "insights", cardName = "insights", isSelected = true)
        val Assets = View("Assets", "assets", "assets")
        val Errors = View("Errors", "errors", "errors")
        val ErrorDetails = View(title = "Error Details", id = "errorsDetails", cardName = "errors", isHidden = true)
        val Tests = View("Tests", "tests", "tests")


        val views = listOf(Insights, Assets, Errors, ErrorDetails, Tests)

        fun findById(id: String): View? {
            return views.find { it.id == id }
        }

        fun setSelected(view: View) {
            views.forEach { v ->
                v.isSelected = view.id == v.id
            }
        }

        fun getSelected(): View? {
            return views.find { it.isSelected }
        }

        fun hideErrorDetails() {
            views.forEach { v ->
                v.isHidden = v == ErrorDetails
            }
        }

        fun hideErrors() {
            views.forEach { v ->
                v.isHidden = v == Errors
            }
        }
    }
}


@Service(Service.Level.PROJECT)
class MainContentViewSwitcher(val project: Project) {

    private lateinit var myLayout: CardLayout
    private lateinit var mainContentPanel: Container


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


    fun showView(view: View, isTriggeredByJcef: Boolean = false) {
        showView(view, fireEvent = true, isTriggeredByJcef = isTriggeredByJcef)
    }

    private fun showView(view: View, fireEvent: Boolean, isTriggeredByJcef: Boolean) {

        if (view == View.ErrorDetails) {
            hideErrors()
        } else {
            project.service<ErrorsViewOrchestrator>().closeErrorDetails()
            hideErrorDetails()
        }

        if (view == View.Insights && getSelected() != View.Insights) {
            ActivityMonitor.getInstance(project).clearLastInsightsViewed()
        }


        setSelected(view)
        myLayout.show(mainContentPanel, view.cardName)
        getSelected()?.takeIf { fireEvent }?.let {
            fireViewChanged(views, isTriggeredByJcef)
        }
    }


    private fun fireViewChanged(views: List<View>, isTriggeredByJcef: Boolean) {
        val publisher = project.messageBus.syncPublisher(ViewChangedEvent.VIEW_CHANGED_TOPIC)
        publisher.viewChanged(views, isTriggeredByJcef)

    }

    fun getSelectedView(): View? {
        return getSelected()
    }

    fun showViewById(vuid: String, isTriggeredByJcef: Boolean = false) {
        val view = View.findById(vuid)
        view?.let { vu ->
            showView(vu, isTriggeredByJcef = isTriggeredByJcef)
        }
    }
}