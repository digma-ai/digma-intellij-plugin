package org.digma.intellij.plugin.insights

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.insights.view.BuildersHolder
import org.digma.intellij.plugin.insights.view.InsightsViewBuilder
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.InsightType
import org.digma.intellij.plugin.model.discovery.CodeLessSpan
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.model.rest.insights.InsightsOfSingleSpanResponse

class CodeLessSpanInsightsProvider(private val codeLessSpan: CodeLessSpan, private val project: Project) {


    private val logger: Logger = Logger.getInstance(javaClass)

    private fun getObject(): CodeLessSpan {
        return codeLessSpan
    }


    private fun getObjectIdWithType(): String {
        return codeLessSpan.spanId
    }


    fun getInsights(): CodelessSpanInsightsContainer? {

        val analyticsService = AnalyticsService.getInstance(project)
        val objectId = getObjectIdWithType()

        try {

            Log.log(logger::debug, project, "requesting insights for {}", getObject())
            val insightsResponse: InsightsOfSingleSpanResponse = analyticsService.getInsightsForSingleSpan(objectId)
            Log.log(logger::debug, project, "Got insights for {} [{}]", getObject(), insightsResponse)

            val insightsContainer = getInsightsListContainer(filterUnmapped(insightsResponse.insights))
            return CodelessSpanInsightsContainer(insightsContainer, insightsResponse)

        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "CodeLessSpanInsightsProvider.getInsights", e)
            Log.debugWithException(logger, e, "Cannot load insights for {} Because: {}", getObject(), e.message)
            return null
        }
    }

    private fun getInsightsListContainer(
        insightsList: List<CodeObjectInsight>,
    ): InsightsListContainer {
        val insightsViewBuilder = InsightsViewBuilder(BuildersHolder())
        val listViewItems = insightsViewBuilder.build(project, insightsList)
        Log.log(logger::debug, "ListViewItems for {}: {}", getObject(), listViewItems)
        return InsightsListContainer(listViewItems, insightsList.size)
    }


    private fun filterUnmapped(codeObjectInsights: List<CodeObjectInsight>): List<CodeObjectInsight> {
        return codeObjectInsights.filter { it.type != InsightType.Unmapped }
    }





}