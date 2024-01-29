package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.document.DocumentInfoContainer
import org.digma.intellij.plugin.model.InsightType
import org.digma.intellij.plugin.model.discovery.CodeLessSpan
import org.digma.intellij.plugin.model.discovery.EndpointInfo
import org.digma.intellij.plugin.model.discovery.MethodInfo
import java.io.InputStream
import javax.swing.JComponent

interface InsightsService : Disposable {

    fun getComponent(): JComponent
    fun showInsight(spanId: String)
    fun openHistogram(instrumentationLibrary: String, name: String, insightType: String)
    fun openLiveView(prefixedCodeObjectId: String)
    fun isIndexHtml(path: String): Boolean
    fun buildIndexFromTemplate(path: String): InputStream?
    fun updateInsights(codeLessSpan: CodeLessSpan)
    fun updateInsights(methodInfo: MethodInfo)
    fun updateInsights(endpointInfo: EndpointInfo)
    fun refreshInsights()
    fun showDocumentPreviewList(documentInfoContainer: DocumentInfoContainer?, fileUri: String)
    fun recalculate(prefixedCodeObjectId: String, insightType: String)
    fun refresh(insightType: InsightType)
    fun goToTrace(traceId: String, traceName: String, insightType: InsightType, spanCodeObjectId: String?)
    fun goToTraceComparison(traceId1: String, traceName1: String, traceId2: String, traceName2: String, insightType: InsightType)
    fun addAnnotation(methodId: String)
    fun fixMissingDependencies(methodId: String)

    companion object {
        @JvmStatic
        fun getInstance(project: Project): InsightsService {
            return project.service<InsightsService>()
        }
    }
}