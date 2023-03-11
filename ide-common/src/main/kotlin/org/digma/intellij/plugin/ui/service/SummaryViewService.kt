package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBusConnection
import org.digma.intellij.plugin.analytics.EnvironmentChanged
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.Models
import org.digma.intellij.plugin.model.rest.insights.GlobalInsight
import org.digma.intellij.plugin.model.rest.insights.SpanDurationChangeInsight
import org.digma.intellij.plugin.model.rest.insights.TopErrorFlowsInsight
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult
import org.digma.intellij.plugin.psi.LanguageService
import org.digma.intellij.plugin.summary.SummariesProvider
import org.digma.intellij.plugin.ui.model.PanelModel
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.util.*
import java.util.concurrent.locks.ReentrantLock


class SummaryViewService(project: Project) : AbstractViewService(project) {

    private val logger: Logger = Logger.getInstance(SummaryViewService::class.java)

    private val summariesProvider: SummariesProvider = project.getService(SummariesProvider::class.java)

    val model = Model()

    private val rebuildPanelLock = ReentrantLock()
    private val environmentChangeConnection: MessageBusConnection = project.messageBus.connect()

    companion object {
        fun getInstance(project: Project): SummaryViewService {
            return project.getService(SummaryViewService::class.java)
        }
    }

    init {

        //StartupActivity runs in smart mode and is ok for java, python,
        // but in rider smart mode doesn't necessarily mean that the backend solution was fully loaded.
        // runWhenSmartForAll will add the task for every registered language service and each will execute it.
        // usually there is only one registered language service, but if python is installed on Rider there will be
        // C# and python language service, same if python is installed on idea. worst case the reload will be called
        // more than once.
        LanguageService.runWhenSmartForAll(project){
            Log.log(logger::debug, "runWhenSmart called")
            reloadSummariesPanelInBackground(project)
        }


        //this is for when environment changes or connection lost and regained
        environmentChangeConnection.subscribe(
            EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC,
            object : EnvironmentChanged {

                override fun environmentChanged(newEnv: String?) {
                    Log.log(logger::debug, "environmentChanged called")
                    reloadSummariesPanelInBackground(project)
                }

                override fun environmentsListChanged(newEnvironments: MutableList<String>?) {
                    Log.log(logger::debug, "environmentsListChanged called")
                    reloadSummariesPanelInBackground(project)
                }
            })
    }


    override fun dispose() {
        super.dispose()
        environmentChangeConnection.dispose()
    }

    //the summary view can always update its ui because it's not related to the current method
    //its triggered by environmentChanged and should always be ok to update
    override fun canUpdateUI(): Boolean {
        return true
    }

    private fun reloadSummariesPanelInBackground(project: Project) {
        Log.log(logger::debug, "reloadSummariesPanelInBackground called")
        val task = Runnable {
            rebuildPanelLock.lock()
            Log.log(logger::debug, "Lock acquired for reload Summaries panel process.")
            try {
                reload()
            } finally {
                rebuildPanelLock.unlock()
                Log.log(logger::debug, "Lock released for reload Summaries panel process.")
            }
        }
        Backgroundable.ensureBackground(project, "Summary view Reload", task)
    }

    private fun reload() {
        Log.log(logger::debug, "reload called")
        val insights = summariesProvider.globalInsights
        val environmentStatuses = summariesProvider.environmentStatuses
        model.insights = insights
        model.usageStatusResult = UsageStatusResult(emptyList(), environmentStatuses)
        model.count = countElementsOnSummaryTab(insights)
        updateUi()
    }

    fun empty() {
        Log.log(logger::debug, "empty called")
        model.insights = Collections.emptyList()
        model.usageStatusResult = Models.Empties.EmptyUsageStatusResult
        model.count = 0
        updateUi()
    }

    override fun getViewDisplayName(): String {
        return "Summary" + if (model.count > 0) " (${model.count})" else ""
    }

    class Model : PanelModel {
        var insights: List<ListViewItem<GlobalInsight>> = Collections.emptyList()
        var count = 0
        var usageStatusResult: UsageStatusResult = Models.Empties.EmptyUsageStatusResult

        override fun count(): String = count.toString()

        override fun isMethodScope(): Boolean = false

        override fun isDocumentScope(): Boolean = false

        override fun getScope(): String = "Current environment"

        override fun getScopeTooltip(): String = ""

        override fun getUsageStatus(): UsageStatusResult = usageStatusResult
    }

    private fun countElementsOnSummaryTab(insights: List<ListViewItem<*>>): Int {
        var counter = 0
        for (insight in insights) {
            when (val model = insight.modelObject) {
                is TopErrorFlowsInsight -> {
                    for (error in model.errors) {
                        counter ++
                    }
                }
                is SpanDurationChangeInsight -> {
                    for (change in model.spanDurationChanges) {
                        val changedPercentiles = change.percentiles.filter { needToShowDurationChange(it) }
                        if (changedPercentiles.isNotEmpty()) {
                            counter ++
                        }
                    }
                }
            }
        }
        return counter
    }
}

