package org.digma.intellij.plugin.ui.jaegerui

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.jaegerui.AbstractJaegerUIService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.navigation.codenavigation.CodeNavigator
import org.digma.intellij.plugin.psi.LanguageServiceProvider
import org.digma.intellij.plugin.ui.jaegerui.model.GoToSpanMessage
import org.digma.intellij.plugin.ui.jaegerui.model.Insight
import org.digma.intellij.plugin.ui.jaegerui.model.Span
import org.digma.intellij.plugin.ui.jaegerui.model.SpanData
import org.digma.intellij.plugin.ui.jaegerui.model.SpansMessage

@Service(Service.Level.PROJECT)
class JaegerUIService(project: Project) : AbstractJaegerUIService(project) {

    companion object {
        @JvmStatic
        fun getInstance(project: Project): JaegerUIService {
            return project.service<JaegerUIService>()
        }
    }

    suspend fun navigateToCode(goToSpanMessage: GoToSpanMessage) {
        Log.log(logger::trace, project, "goToSpan request {}", goToSpanMessage)
        val span = goToSpanMessage.payload
        CodeNavigator.getInstance(project).maybeNavigateToSpanOrMethod(span.spanCodeObjectId, span.methodCodeObjectId)
    }


    suspend fun getResolvedSpans(spansMessage: SpansMessage): Map<String, SpanData> {
        val allSpans = mutableMapOf<String, SpanData>()

        val spanCodeObjectIds = spansMessage.payload.spans.mapNotNull { it.spanCodeObjectId }.toList()
        val methodIds = spansMessage.payload.spans.mapNotNull { it.methodId() }.toList()
        val spanCodeObjectIdsNoPrefix = spansMessage.payload.spans.map { it.spanId() }.toList()

        val allInsights = getInsights(spanCodeObjectIds)

        for (languageService in LanguageServiceProvider.getInstance(project).getLanguageServices()) {
            val spanWorkspaceUris = languageService.findWorkspaceUrisForSpanIds(spanCodeObjectIdsNoPrefix)
            val methodWorkspaceUris = languageService.findWorkspaceUrisForMethodCodeObjectIds(methodIds)
            spansMessage.payload.spans.forEach { span: Span ->
                val spanId = span.spanId()
                val methodId = span.methodId()
                val hasCodeLocation = (spanWorkspaceUris.containsKey(spanId) || methodWorkspaceUris.containsKey(methodId))

                val spanData = allSpans.computeIfAbsent(span.id) { s: String -> SpanData(hasCodeLocation, mutableListOf()) }
                addInsightsToSpanData(spanData, span.spanCodeObjectId, methodId, allInsights)
            }
        }

        return allSpans
    }

    private fun addInsightsToSpanData(
        spanData: SpanData,
        spanId: String?,
        methodId: String?,
        allInsights: Map<String, List<Insight>>
    ) {

        val combined = buildList {
            addAll(spanData.insights)
            spanId?.let { addAll(allInsights[it].orEmpty()) }
            methodId?.let { addAll(allInsights[it].orEmpty()) }
        }

        spanData.insights.apply {
            clear()
            addAll(distinctByType(combined))
        }
    }

    private fun distinctByType(spanInsights: List<Insight>): List<Insight> {
        return spanInsights
            .groupBy { it.type }
            .mapNotNull { (_, group) -> group.minByOrNull { it.importance } }.toList()
    }

}