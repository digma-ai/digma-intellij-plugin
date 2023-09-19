package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBusConnection
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.analytics.EnvironmentChanged
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.Models
import org.digma.intellij.plugin.model.rest.insights.GlobalInsight
import org.digma.intellij.plugin.model.rest.insights.SpanDurationChangeInsight
import org.digma.intellij.plugin.model.rest.insights.TopErrorFlowsInsight
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult
import org.digma.intellij.plugin.psi.LanguageService
import org.digma.intellij.plugin.settings.SettingsState
import org.digma.intellij.plugin.summary.SummariesProvider
import org.digma.intellij.plugin.ui.model.CurrentEnvironmentScope
import org.digma.intellij.plugin.ui.model.PanelModel
import org.digma.intellij.plugin.ui.model.Scope
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import org.digma.intellij.plugin.ui.needToShowDurationChange
import java.util.Collections
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.locks.ReentrantLock


class SummaryViewService(project: Project) : AbstractViewService(project) {

    private val logger: Logger = Logger.getInstance(SummaryViewService::class.java)

    private val summariesProvider: SummariesProvider = project.getService(SummariesProvider::class.java)
    private val settingsState: SettingsState = SettingsState.getInstance()
    private val time: Timer = Timer()

    val model = Model()

    private val rebuildPanelLock = ReentrantLock()
    private val environmentChangeConnection: MessageBusConnection = project.messageBus.connect()

    companion object {
        private val logger = Logger.getInstance(SummaryViewService::class.java)
        fun getInstance(project: Project): SummaryViewService {
            logger.warn("Getting instance of ${SummaryViewService::class.simpleName}")
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
        LanguageService.runWhenSmartForAll(project) {
            Log.log(logger::debug, "runWhenSmart called")
            val reloadTask = object : TimerTask() {
                override fun run() {
                    if (BackendConnectionMonitor.getInstance(project).isConnectionOk()) {
                        try {
                            reloadSummariesPanel()
                        } catch (e: Exception) {
                            Log.warnWithException(logger, e, "Exception in reloadSummariesPanel")
                            ErrorReporter.getInstance().reportError(project, "SummaryViewService.reloadSummariesPanel", e)
                        }
                    }
                }
            }
            time.schedule(reloadTask, 0, settingsState.refreshDelay * 1000L)
        }


        //this is for when environment changes or connection lost and regained
        environmentChangeConnection.subscribe(
            EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC,
            object : EnvironmentChanged {

                override fun environmentChanged(newEnv: String?, refreshInsightsView: Boolean) {
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
        time.cancel()
        environmentChangeConnection.dispose()
    }

    //the summary view can always update its ui because it's not related to the current method
    //its triggered by environmentChanged and should always be ok to update
    override fun canUpdateUI(): Boolean {
        return true
    }

    private fun reloadSummariesPanelInBackground(project: Project) {
        Log.log(logger::trace, "reloadSummariesPanelInBackground called")
        val task = Runnable {
            reloadSummariesPanel()
        }
        Backgroundable.ensureBackground(project, "Summary view Reload", task)
    }

    private fun reloadSummariesPanel() {
        rebuildPanelLock.lock()
        Log.log(logger::trace, "Lock acquired for reload Summaries panel process.")
        try {
            reload()
        } finally {
            rebuildPanelLock.unlock()
            Log.log(logger::trace, "Lock released for reload Summaries panel process.")
        }
    }


    private fun reload() {
        Log.log(logger::trace, "reload called")
        var insights = summariesProvider.globalInsights
        //todo:limit by size because long lists cause UI freeze
        if (insights.size > 10) {
            insights = insights.subList(0, 10)
        }

        val environmentStatuses = summariesProvider.environmentStatuses
        model.insights = insights
        model.usageStatusResult = UsageStatusResult(emptyList(), environmentStatuses)
        model.count = countElementsOnSummaryTab(insights)
        updateUi()
    }

    fun empty() {
        Log.log(logger::trace, "empty called")
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
        var scope = CurrentEnvironmentScope()

        override fun count(): String = count.toString()

        override fun getTheScope(): Scope {
            return scope
        }

        override fun isMethodScope(): Boolean = false

        override fun isDocumentScope(): Boolean = false

        override fun isCodeLessSpanScope(): Boolean = false

        override fun getScopeString(): String = scope.getScope()

        override fun getScopeTooltip(): String = scope.getScopeTooltip()

        override fun getUsageStatus(): UsageStatusResult = usageStatusResult
    }

    private fun countElementsOnSummaryTab(insights: List<ListViewItem<*>>): Int {
        var counter = 0
        for (insight in insights) {
            when (val model = insight.modelObject) {
                is TopErrorFlowsInsight -> {
                    for (error in model.errors) {
                        counter++
                    }
                }

                is SpanDurationChangeInsight -> {
                    for (change in model.spanDurationChanges) {
                        val changedPercentiles = change.percentiles.filter { needToShowDurationChange(it) }
                        if (changedPercentiles.isNotEmpty()) {
                            counter++
                        }
                    }
                }
            }
        }
        return counter
    }
}

