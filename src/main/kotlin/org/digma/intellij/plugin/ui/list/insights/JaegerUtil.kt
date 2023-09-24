package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.common.CommonUtils
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.htmleditor.DigmaHTMLEditorProvider
import org.digma.intellij.plugin.jaegerui.JaegerUIService
import org.digma.intellij.plugin.model.InsightType
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.settings.LinkMode
import org.digma.intellij.plugin.settings.SettingsState
import org.digma.intellij.plugin.ui.list.ListItemActionButton
import org.digma.intellij.plugin.ui.model.TraceSample
import java.util.Collections
import javax.swing.JButton

const val traceButtonName: String = "show-in-jaeger"

// if cannot create the button then would return null
fun buildButtonToJaeger(
        project: Project, linkCaption: String, spanName: String, traceSamples: List<TraceSample?>, insightType: InsightType
): JButton? {

    val filteredTraces = traceSamples.filter { traceSample -> traceSample != null && traceSample.hasTraceId() }

    if (!isJaegerButtonEnabled() || filteredTraces.isEmpty()){
        return null
    }

    val button = ListItemActionButton(linkCaption)
    button.addActionListener{

        ActivityMonitor.getInstance(project).registerButtonClicked(traceButtonName, insightType)

        val settingsState = SettingsState.getInstance()
        val jaegerBaseUrl = settingsState.jaegerUrl?.trim()?.trimEnd('/')
        val jaegerUrl: String
        val embedPart = "&uiEmbed=v0"

        val trace1 = filteredTraces[0]!!.traceId?.lowercase()
        jaegerUrl = if (filteredTraces.size == 1) {
            "${jaegerBaseUrl}/trace/${trace1}?cohort=${trace1}${embedPart}"
        } else {
            // assuming it has (at least) size of 2
            val trace2 = filteredTraces[1]!!.traceId?.lowercase()
            "${jaegerBaseUrl}/trace/${trace1}...${trace2}?cohort=${trace1}&cohort=${trace2}${embedPart}"
        }


        when(settingsState.jaegerLinkMode){

            LinkMode.Internal -> {

                val caption: String = if (filteredTraces.size == 1) {
                    "A sample ${filteredTraces[0]!!.traceName} trace"
                } else {
                    "Comparing: A sample ${filteredTraces[0]!!.traceName} trace with a ${filteredTraces[1]!!.traceName} trace"
                }

                val htmlContent = SpanPanels.JAEGER_EMBEDDED_HTML_TEMPLATE
                    .replace("__JAEGER_EMBEDDED_URL__", jaegerUrl)
                    .replace("__CAPTION__", caption)
                val editorTitle = "Jaeger sample traces of Span $spanName"
                EDT.ensureEDT {
                    DigmaHTMLEditorProvider.openEditor(project, editorTitle, htmlContent)
                }
            }
            LinkMode.External -> {
                EDT.ensureEDT{
                    BrowserUtil.browse(jaegerUrl, project)
                }
            }
            LinkMode.Embedded -> {
                JaegerUIService.getInstance(project).openEmbeddedJaeger(filteredTraces,spanName)
            }
        }
    }


    return button
}






fun openJaegerFromRecentActivity(
        project: Project,
        traceId: String,
        spanName: String
) {

    if (!isJaegerButtonEnabled() || traceId.isBlank()){
        return
    }

    val settingsState = SettingsState.getInstance()

    val jaegerUrlEmbedPart = "&uiEmbed=v0"
    val jaegerBaseUrl = settingsState.jaegerUrl?.trim()?.trimEnd('/')
    val jaegerUrl = "${jaegerBaseUrl}/trace/${traceId.lowercase()}?cohort=${traceId.lowercase()}${jaegerUrlEmbedPart}"

    when(settingsState.jaegerLinkMode){

        LinkMode.Internal -> {
            val caption = "A sample $spanName trace"
            val htmlContent = SpanPanels.JAEGER_EMBEDDED_HTML_TEMPLATE
                .replace("__JAEGER_EMBEDDED_URL__", jaegerUrl)
                .replace("__CAPTION__", caption)
            val editorTitle = "Jaeger sample traces of Span $spanName"
            EDT.ensureEDT {
                DigmaHTMLEditorProvider.openEditor(project, editorTitle, htmlContent)
            }
        }
        LinkMode.External -> {
            EDT.ensureEDT{
                BrowserUtil.browse(jaegerUrl, project)
            }
        }

        LinkMode.Embedded -> {
            JaegerUIService.getInstance(project).openEmbeddedJaeger(traceId, spanName)
        }
    }

}


fun openJaegerFromInsight(
    project: Project,
    traceId: String,
    traceName: String,
    insightType: InsightType,
) {

    ActivityMonitor.getInstance(project).registerButtonClicked(traceButtonName, insightType)

    val settingsState = SettingsState.getInstance()
    val jaegerBaseUrl = settingsState.jaegerUrl?.trim()?.trimEnd('/')
    val jaegerUrl: String
    val embedPart = "&uiEmbed=v0"

    val trace1 = traceId.lowercase();
    if (jaegerBaseUrl!=null && jaegerBaseUrl.contains("\${TRACE_ID}")){
        jaegerUrl=jaegerBaseUrl.replace("\${TRACE_ID}",trace1)
    }
    else{
        jaegerUrl = "${jaegerBaseUrl}/trace/${trace1}?cohort=${trace1}${embedPart}"
    }

    when (settingsState.jaegerLinkMode) {

        LinkMode.Internal -> {

            val caption = "A sample ${traceName} trace"

            val htmlContent = SpanPanels.JAEGER_EMBEDDED_HTML_TEMPLATE
                .replace("__JAEGER_EMBEDDED_URL__", jaegerUrl)
                .replace("__CAPTION__", caption)
            val editorTitle = "Jaeger sample traces of Span $traceName"
            EDT.ensureEDT {
                DigmaHTMLEditorProvider.openEditor(project, editorTitle, htmlContent)
            }
        }

        LinkMode.External -> {
            EDT.ensureEDT {
                BrowserUtil.browse(jaegerUrl, project)
            }
        }

        LinkMode.Embedded -> {
            val traceSample = TraceSample(traceName, traceId);
            JaegerUIService.getInstance(project).openEmbeddedJaeger(Collections.singletonList(traceSample), traceName)
        }
    }
}


fun openJaegerComparisonFromInsight(
    project: Project,
    traceId1: String,
    traceName1: String,
    traceId2: String,
    traceName2: String,
    insightType: InsightType,
) {

    ActivityMonitor.getInstance(project).registerButtonClicked(traceButtonName, insightType)

    val settingsState = SettingsState.getInstance()
    val jaegerBaseUrl = settingsState.jaegerUrl?.trim()?.trimEnd('/')
    val jaegerUrl: String
    val embedPart = "&uiEmbed=v0"

    jaegerUrl =
        "${jaegerBaseUrl}/trace/${traceId1.lowercase()}...${traceId2.lowercase()}?cohort=${traceId1.lowercase()}&cohort=${traceId2.lowercase()}${embedPart}"

    when (settingsState.jaegerLinkMode) {

        LinkMode.Internal -> {

            val caption = "Comparing: A sample ${traceName1} trace with a ${traceName2} trace"

            val htmlContent = SpanPanels.JAEGER_EMBEDDED_HTML_TEMPLATE
                .replace("__JAEGER_EMBEDDED_URL__", jaegerUrl)
                .replace("__CAPTION__", caption)
            val editorTitle = "Jaeger sample traces of Span $traceName1"
            EDT.ensureEDT {
                DigmaHTMLEditorProvider.openEditor(project, editorTitle, htmlContent)
            }
        }

        LinkMode.External -> {
            EDT.ensureEDT {
                BrowserUtil.browse(jaegerUrl, project)
            }
        }

        LinkMode.Embedded -> {
            val traceSample1 = TraceSample(traceName1, traceId1);
            val traceSample2 = TraceSample(traceName2, traceId2);
            val traces = listOf(traceSample1, traceSample2)
            JaegerUIService.getInstance(project).openEmbeddedJaeger(traces, traceName1)
        }
    }
}


fun isJaegerButtonEnabled(): Boolean {
    val settingsState = SettingsState.getInstance()
    return settingsState.jaegerLinkMode == LinkMode.Embedded ||
            (!settingsState.jaegerUrl.isNullOrBlank() && CommonUtils.isWelFormedUrl(settingsState.jaegerUrl))
}