package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.EnvironmentChanged
import org.digma.intellij.plugin.common.DumbAwareNotifier
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.Models
import org.digma.intellij.plugin.model.rest.insights.GlobalInsight
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult
import org.digma.intellij.plugin.summary.SummariesProvider
import org.digma.intellij.plugin.ui.model.PanelModel
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.util.*


class SummaryViewService(project: Project) : AbstractViewService(project) {

    private val logger: Logger = Logger.getInstance(ErrorsViewService::class.java)
    private val summariesProvider: SummariesProvider = project.getService(SummariesProvider::class.java)

    val model = Model()

    companion object {
        fun getInstance(project: Project): SummaryViewService {
            return project.getService(SummaryViewService::class.java)
        }
    }

    init {

        //this is for startup
        DumbAwareNotifier.getInstance(project).whenSmart {
            reload()
        }

        //this is for when environment changes or connection lost and regained
        project.messageBus.connect(project)
            .subscribe(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC, object : EnvironmentChanged {

                override fun environmentChanged(newEnv: String?) {
                    Log.log(logger::debug, "environmentChanged called")
                    reload()
                }

                override fun environmentsListChanged(newEnvironments: MutableList<String>?) {
                    Log.log(logger::debug, "environmentsListChanged called")
                    reload()
                }
            })
    }


    //the summary view can always update its ui because it's not related to the current method
    //its triggered by environmentChanged and should always be ok to update
    override fun canUpdateUI(): Boolean {
        return true
    }


    private fun reload() {
        Log.log(logger::debug, "reload called")
        val insights = summariesProvider.getGlobalInsights()
        val environmentStatuses = summariesProvider.getEnvironmentStatuses()
        model.insights = insights
        model.usageStatusResult = UsageStatusResult(emptyList(), environmentStatuses)
        model.count = insights.sumOf { it.modelObject.count() }
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


}

