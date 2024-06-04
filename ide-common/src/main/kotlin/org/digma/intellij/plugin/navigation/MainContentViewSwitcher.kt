package org.digma.intellij.plugin.navigation

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.base.Objects
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.navigation.View.Companion.Analytics
import org.digma.intellij.plugin.navigation.View.Companion.Assets
import org.digma.intellij.plugin.navigation.View.Companion.Errors
import org.digma.intellij.plugin.navigation.View.Companion.Highlights
import org.digma.intellij.plugin.navigation.View.Companion.Tests
import org.digma.intellij.plugin.navigation.View.Companion.getSelected
import org.digma.intellij.plugin.navigation.View.Companion.setSelected
import org.digma.intellij.plugin.navigation.View.Companion.views
import org.digma.intellij.plugin.posthog.ActivityMonitor

//todo: this class is still used while transitioning to a single jcef app but should be removed at some point
@Suppress("unused")
@Service(Service.Level.PROJECT)
class MainContentViewSwitcher(val project: Project) {


    companion object {

        @JvmStatic
        fun getInstance(project: Project): MainContentViewSwitcher {
            return project.service<MainContentViewSwitcher>()
        }
    }


    fun showInsights() {
        showView(View.Insights)
    }

    fun showAssets() {
        showView(Assets)
    }

    fun showErrors() {
        showView(Errors)
    }

    fun showTests() {
        showView(Tests)
    }

    fun showAnalytics() {
        showView(Analytics)
    }

    fun showHighlights() {
        showView(Highlights)
    }


    fun showView(view: View, createHistoryStep: Boolean = false) {
        showView(view, fireEvent = true, createHistoryStep)
    }

    private fun showView(view: View, fireEvent: Boolean, createHistoryStep: Boolean) {

        if (view != Assets) {
            Assets.path = null
        }

        if (view != Errors) {
            Errors.path = null
        }

        if (view == View.Insights && getSelected() != View.Insights) {
            ActivityMonitor.getInstance(project).clearLastInsightsViewed()
        }

        setSelected(view)

        if (fireEvent) {
            fireViewChanged(createHistoryStep)
        }
    }


    private fun fireViewChanged(createHistoryStep: Boolean) {
        val publisher = project.messageBus.syncPublisher(ViewChangedEvent.VIEW_CHANGED_TOPIC)
        publisher.viewChanged(views, createHistoryStep)
    }

    fun getSelectedView(): View? {
        return getSelected()
    }

    fun showViewById(viewId: String, createHistoryStep: Boolean = false) {
        val segments = viewId.split("/")
        if (segments.size > 1 && segments[1] == "assets") {
            if (segments.count() > 2) {
                Assets.path = viewId.removePrefix("/assets/")
            } else {
                Assets.path = null
            }
            showView(Assets, createHistoryStep)
        } else if (segments.size > 1 && segments[1] == "errors") {
            if (segments.count() > 2) {
                Errors.path = viewId.removePrefix("/errors/")
            } else {
                Errors.path = null
            }
            showView(Errors, createHistoryStep)
        } else {
            View.findById(viewId)?.let { view ->
                showView(view, createHistoryStep)
            }
        }
    }

}

//todo: we don't need View anymore , but its left according to Kyrylo for less changes while transitioning to a single jcef app.
// needs to be deleted at some point.
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
    var path: String? = null,
) {

    override fun equals(other: Any?): Boolean {
        return other is View && other.id == id && other.title == title && other.cardName == cardName
    }

    override fun hashCode(): Int {
        return Objects.hashCode(id, title)
    }

    companion object {

        @JvmStatic
        val Highlights = View(title = "", id = "/highlights", cardName = "highlights")

        @JvmStatic
        val Insights = View(title = "Issues", id = "/insights", cardName = "insights", isSelected = true)

        @JvmStatic
        val Assets = View("Assets", "/assets", "assets")

        @JvmStatic
        val Errors = View("Errors", "/errors", "errors")

        @JvmStatic
        val Tests = View("Tests", "/tests", "tests")

        @JvmStatic
        val Analytics = View("Analytics", "/analytics", "analytics")


        val views = listOf(Highlights, Insights, Assets, Analytics, Errors, Tests)

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

    }
}
