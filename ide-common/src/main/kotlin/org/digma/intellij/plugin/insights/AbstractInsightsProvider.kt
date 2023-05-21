package org.digma.intellij.plugin.insights

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.errors.ErrorsListContainer
import org.digma.intellij.plugin.insights.view.BuildersHolder
import org.digma.intellij.plugin.insights.view.InsightsViewBuilder
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.InsightType
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult
import org.digma.intellij.plugin.ui.model.listview.ListViewItem

abstract class AbstractInsightsProvider(val project: Project) {

    private val logger: Logger = Logger.getInstance(javaClass)

    abstract fun getObject():Any

    abstract fun getObjectIdsWithType():List<String>

    //we want to use existing infrastructures for building the list on insights, until more refactoring is done
    // for the new navigation we need to provide a fake or possible MethodInfo.
    // InsightsViewBuilder.build need a MethodInfo and expects it to be non-null and with real values.
    abstract fun getNonNulMethodInfo(): MethodInfo

    fun getInsights(): InsightsListContainer? {

        val analyticsService = AnalyticsService.getInstance(project)
        val objectIds = getObjectIdsWithType()

        try {

            Log.log(logger::debug,project,"requesting insights for {}",getObject())
            val insights :List<CodeObjectInsight>  = analyticsService.getInsights(objectIds)
            Log.log(logger::debug,project,"Got insights for {} [{}]",getObject(),insights)

            Log.log(logger::debug,project,"requesting usageStatus for {}",getObject())
            val usageStatus:UsageStatusResult = analyticsService.getUsageStatus(objectIds)
            Log.log(logger::debug,project,"Got usageStatus for {} [{}]",getObject(),usageStatus)

            Log.log(logger::debug,project,"requesting usageStatusOfErrors for {}",getObject())
            val usageStatusOfErrors:UsageStatusResult = analyticsService.getUsageStatusOfErrors(objectIds)
            Log.log(logger::debug,project,"Got usageStatusOfErrors for {} [{}]",getObject(),usageStatusOfErrors)

            return getInsightsListContainer(filterUnmapped(insights), usageStatus)

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
        val listViewItems = insightsViewBuilder.build(project, getNonNulMethodInfo(), insightsList)
        Log.log(logger::debug, "ListViewItems for {}: {}", getObject(), listViewItems)
        return InsightsListContainer(listViewItems, insightsList.size, usageStatus)
    }


    private fun filterUnmapped(codeObjectInsights: List<CodeObjectInsight>): List<CodeObjectInsight> {
        return codeObjectInsights.filter { it.type != InsightType.Unmapped }
    }


    fun getErrors(): ErrorsListContainer? {

        val analyticsService = AnalyticsService.getInstance(project)
        val objectIds = getObjectIdsWithType()

        try {
            Log.log(logger::debug,project,"requesting errors for {}",getObject())
            val codeObjectErrors: List<CodeObjectError> = analyticsService.getErrorsOfCodeObject(objectIds)
            Log.log(logger::debug,project,"Got errors for {} [{}]",getObject(),codeObjectErrors)

            val errorsListViewItems = codeObjectErrors.map { ListViewItem(it, 1) }
            Log.log(logger::debug, "ListViewItems for {}: {}", getObject(), errorsListViewItems)

            val usageStatus = analyticsService.getUsageStatusOfErrors(objectIds)
            Log.log(logger::debug, "UsageStatus for {}: {}", getObject(), usageStatus)

            return ErrorsListContainer(errorsListViewItems, usageStatus)
        } catch (e: AnalyticsServiceException){
            Log.debugWithException(logger,e, "Cannot load errors for {} Because: {}",getObject() , e.message)
            return null
        }
    }


}