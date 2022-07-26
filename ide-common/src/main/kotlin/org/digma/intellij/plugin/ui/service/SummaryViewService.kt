package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.insights.GlobalInsight
import org.digma.intellij.plugin.summary.SummariesProvider
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.util.Collections


class SummaryViewService(project: Project) : AbstractViewService(project) {

    private val logger: Logger = Logger.getInstance(ErrorsViewService::class.java)
    private val summariesProvider: SummariesProvider = project.getService(SummariesProvider::class.java)

    val model = Model()

    companion object {
        fun getInstance(project: Project): SummaryViewService {
            return project.getService(SummaryViewService::class.java)
        }
    }

    fun environmentChanged() {
        Log.log(logger::debug, "environmentChanged called")
        val insights = summariesProvider.getGlobalInsights()
        model.insights = insights
        model.count = insights.sumOf { it.modelObject.count() }
        updateUi()
    }

    fun empty() {
        Log.log(logger::debug, "empty called")
        model.insights = Collections.emptyList()
        model.count = 0
        updateUi()
    }

    override fun getViewDisplayName(): String {
        return "Summary" + if (model.count > 0) " (${model.count})" else ""
    }

    class Model {
        var insights: List<ListViewItem<GlobalInsight>> = Collections.emptyList()
        var count = 0
    }
}

