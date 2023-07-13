package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.model.discovery.CodeLessSpan
import org.digma.intellij.plugin.model.discovery.MethodInfo
import java.io.InputStream
import javax.swing.JComponent

interface InsightsService : Disposable {

    fun getComponent(): JComponent?
    fun showInsight(spanId: String)
    fun openHistogram(instrumentationLibrary: String, name: String, insightType: String)
    fun openLiveView(prefixedCodeObjectId: String)
    fun isIndexHtml(path: String): Boolean
    fun buildIndexFromTemplate(path: String): InputStream?
    fun updateInsights(codeLessSpan: CodeLessSpan)
    fun updateInsights(methodInfo: MethodInfo)
    fun refreshInsights()

    companion object {
        @JvmStatic
        fun getInstance(project: Project): InsightsService {
            return project.service<InsightsService>()
        }
    }
}