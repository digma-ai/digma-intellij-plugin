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
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.model.rest.insights.ErrorInsight
import org.digma.intellij.plugin.model.rest.insights.InsightsOfSingleSpanResponse
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult
import org.digma.intellij.plugin.ui.model.listview.ListViewItem

@Deprecated("remove")
abstract class AbstractInsightsProvider(val project: Project) {

    private val logger: Logger = Logger.getInstance(javaClass)

    abstract fun getObject():Any

    abstract fun getObjectIdWithType():String


}