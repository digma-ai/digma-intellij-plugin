package org.digma.intellij.plugin.insights

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.errors.ErrorsListContainer
import org.digma.intellij.plugin.insights.view.BuildersHolder
import org.digma.intellij.plugin.insights.view.InsightsViewBuilder
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.InsightType
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.model.rest.insights.ErrorInsight
import org.digma.intellij.plugin.model.rest.insights.InsightsOfSingleSpanResponse
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult
import org.digma.intellij.plugin.ui.model.listview.ListViewItem

abstract class AbstractInsightsProvider(val project: Project) {

    private val logger: Logger = Logger.getInstance(javaClass)

    abstract fun getObject():Any

    abstract fun getObjectIdWithType():String

    //we want to use existing infrastructures for building the list on insights, until more refactoring is done
    // for the new navigation we need to provide a fake or possible MethodInfo.
    // InsightsViewBuilder.build need a MethodInfo and expects it to be non-null and with real values.
    abstract fun getNonNulMethodInfo(): MethodInfo

    fun getInsights(): CodelessSpanInsightsContainer? {

        val analyticsService = AnalyticsService.getInstance(project)
        val objectId = getObjectIdWithType()

        try {

            Log.log(logger::debug,project,"requesting insights for {}",getObject())
            val insightsResponse: InsightsOfSingleSpanResponse = analyticsService.getInsightsForSingleSpan(objectId)
            Log.log(logger::debug,project,"Got insights for {} [{}]",getObject(),insightsResponse)

            Log.log(logger::debug,project,"requesting usageStatus for {}",getObject())
            val usageStatus:UsageStatusResult = analyticsService.getUsageStatus(listOf(objectId))
            Log.log(logger::debug,project,"Got usageStatus for {} [{}]",getObject(),usageStatus)

            val insightsContainer = getInsightsListContainer(filterUnmapped(insightsResponse.insights), usageStatus)
            return CodelessSpanInsightsContainer(insightsContainer,insightsResponse)

        } catch (e: AnalyticsServiceException) {
            Log.debugWithException(logger,e, "Cannot load insights for {} Because: {}",getObject() , e.message)
            return null
        }
    }

    private fun getInsightsListContainer(
        insightsList: List<CodeObjectInsight>,
        usageStatus: UsageStatusResult,
    ): InsightsListContainer {
        val insightsViewBuilder = InsightsViewBuilder(BuildersHolder())
        //todo: remove method
        val listViewItems = insightsViewBuilder.build(project, getNonNulMethodInfo(), insightsList)
        Log.log(logger::debug, "ListViewItems for {}: {}", getObject(), listViewItems)
        return InsightsListContainer(listViewItems, insightsList.size, usageStatus)
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
                .filter { codeObjectInsight -> codeObjectInsight is ErrorInsight }
                .map { codeObjectInsight -> codeObjectInsight.codeObjectId }

            Log.log(logger::debug,project,"requesting errors for {}",getObject())
            val codeObjectErrors: List<CodeObjectError>  = analyticsService.getErrorsOfCodeObject(CodeObjectsUtil.addMethodTypeToIds(codeObjectIdsForErrors))
            Log.log(logger::debug,project,"Got errors for {} [{}]",getObject(),codeObjectErrors)

            val usageStatus = analyticsService.getUsageStatusOfErrors(listOf(objectId))
            Log.log(logger::debug, "UsageStatus for {}: {}", getObject(), usageStatus)

            val errorsListContainer = getErrorsListContainer(codeObjectErrors,usageStatus)

            return CodelessSpanErrorsContainer(errorsListContainer, insightsResponse)
        } catch (e: AnalyticsServiceException){
            Log.debugWithException(logger,e, "Cannot load errors for {} Because: {}",getObject() , e.message)
            return null
        }
    }

    private fun getErrorsListContainer(codeObjectErrors: List<CodeObjectError>, usageStatus: UsageStatusResult): ErrorsListContainer {

        val errorsListViewItems = codeObjectErrors.map { ListViewItem(it, 1) }
        Log.log(logger::debug, "errors ListViewItems for {}: {}", getObject(), errorsListViewItems)

        return ErrorsListContainer(errorsListViewItems, usageStatus)
    }


}