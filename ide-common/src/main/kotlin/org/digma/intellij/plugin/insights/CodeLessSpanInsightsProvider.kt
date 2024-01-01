package org.digma.intellij.plugin.insights

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.errors.ErrorsListContainer
import org.digma.intellij.plugin.insights.view.BuildersHolder
import org.digma.intellij.plugin.insights.view.InsightsViewBuilder
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.InsightType
import org.digma.intellij.plugin.model.discovery.CodeLessSpan
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.model.rest.insights.ErrorInsight
import org.digma.intellij.plugin.model.rest.insights.InsightsOfSingleSpanResponse
import org.digma.intellij.plugin.ui.model.listview.ListViewItem

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
            ErrorReporter.getInstance().reportError("CodeLessSpanInsightsProvider.getInsights", e)
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


    fun getErrors(): CodelessSpanErrorsContainer? {

        val analyticsService = AnalyticsService.getInstance(project)
        val objectId = getObjectIdWithType()

        try {

            val insightsResponse: InsightsOfSingleSpanResponse = analyticsService.getInsightsForSingleSpan(objectId)
            val codeObjectIdsForErrors = insightsResponse.insights
                .filterIsInstance<ErrorInsight>()
                .map { codeObjectInsight -> codeObjectInsight.codeObjectId }

            Log.log(logger::debug, project, "requesting errors for {}", getObject())
            val codeObjectErrors: List<CodeObjectError> =
                analyticsService.getErrorsOfCodeObject(CodeObjectsUtil.addMethodTypeToIds(codeObjectIdsForErrors))
            Log.log(logger::debug, project, "Got errors for {} [{}]", getObject(), codeObjectErrors)

            val errorsListContainer = getErrorsListContainer(codeObjectErrors)

            return CodelessSpanErrorsContainer(errorsListContainer, insightsResponse)
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("CodeLessSpanInsightsProvider.getErrors", e)
            Log.debugWithException(logger, e, "Cannot load errors for {} Because: {}", getObject(), e.message)
            return null
        }
    }

    private fun getErrorsListContainer(codeObjectErrors: List<CodeObjectError>): ErrorsListContainer {

        val errorsListViewItems = codeObjectErrors.map { ListViewItem(it, 1) }
        Log.log(logger::debug, "errors ListViewItems for {}: {}", getObject(), errorsListViewItems)

        return ErrorsListContainer(errorsListViewItems)
    }



}