package org.digma.intellij.plugin.ui.common

import com.google.common.io.CharStreams
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.common.CommonUtils
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.htmleditor.DigmaHTMLEditorProvider
import org.digma.intellij.plugin.jaegerui.JaegerUIService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.posthog.UserActionOrigin
import org.digma.intellij.plugin.settings.LinkMode
import org.digma.intellij.plugin.settings.SettingsState
import org.digma.intellij.plugin.ui.model.TraceSample
import java.io.InputStreamReader
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Collections


fun openJaegerFromRecentActivity(
    project: Project,
    traceId: String,
    spanName: String,
    spanCodeObjectId: String?,
) {

    if (!isJaegerButtonEnabled() || traceId.isBlank()){
        return
    }

    val settingsState = SettingsState.getInstance()

    val jaegerUrlEmbedPart = "&uiEmbed=v0"
    var jaegerUrl: String
    val jaegerBaseUrl = settingsState.jaegerUrl?.trim()?.trimEnd('/')
    if (settingsState.jaegerLinkMode==LinkMode.External && jaegerBaseUrl!=null && jaegerBaseUrl.contains("\${TRACE_ID}")){
        jaegerUrl=jaegerBaseUrl.replace("\${TRACE_ID}",traceId.lowercase())
    }
    else{
        jaegerUrl = "${jaegerBaseUrl}/trace/${traceId.lowercase()}?cohort=${traceId.lowercase()}${jaegerUrlEmbedPart}"
        spanCodeObjectId?.let {
            jaegerUrl = jaegerUrl.plus("&uiFind=").plus(URLEncoder.encode(spanCodeObjectId, StandardCharsets.UTF_8))
        }
    }


    when(settingsState.jaegerLinkMode){

        LinkMode.Internal -> {
            val caption = "A sample $spanName trace"
            val htmlContent = JaegerEmbeddedHtmlTemplate.JAEGER_EMBEDDED_HTML_TEMPLATE
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
            val traceSample = TraceSample(spanName, traceId)
            JaegerUIService.getInstance(project).openEmbeddedJaeger(Collections.singletonList(traceSample), spanName, spanCodeObjectId, true)
        }
    }

}


fun openJaegerFromInsight(
    project: Project,
    traceId: String,
    traceName: String,
    insightType: String,
    spanCodeObjectId: String?,
) {

    ActivityMonitor.getInstance(project).registerUserActionWithOrigin("trace button clicked", UserActionOrigin.Insights)

    val settingsState = SettingsState.getInstance()
    val jaegerBaseUrl = settingsState.jaegerUrl?.trim()?.trimEnd('/')
    var jaegerUrl: String
    val embedPart = "&uiEmbed=v0"

    val trace1 = traceId.lowercase()
    if (settingsState.jaegerLinkMode==LinkMode.External && jaegerBaseUrl!=null && jaegerBaseUrl.contains("\${TRACE_ID}")){
        jaegerUrl = jaegerBaseUrl.replace("\${TRACE_ID}", trace1)
    }
    else{
        jaegerUrl = "${jaegerBaseUrl}/trace/${trace1}?cohort=${trace1}${embedPart}"
        spanCodeObjectId?.let {
            jaegerUrl = jaegerUrl.plus("&uiFind=").plus(URLEncoder.encode(spanCodeObjectId, StandardCharsets.UTF_8))
        }
    }


    when (settingsState.jaegerLinkMode) {

        LinkMode.Internal -> {

            val caption = "A sample $traceName trace"

            val htmlContent = JaegerEmbeddedHtmlTemplate.JAEGER_EMBEDDED_HTML_TEMPLATE
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
            val traceSample = TraceSample(traceName, traceId)
            JaegerUIService.getInstance(project).openEmbeddedJaeger(Collections.singletonList(traceSample), traceName, spanCodeObjectId, true)
        }
    }
}


fun openJaegerComparisonFromInsight(
    project: Project,
    traceId1: String,
    traceName1: String,
    traceId2: String,
    traceName2: String,
    insightType: String,
) {

    ActivityMonitor.getInstance(project).registerUserActionWithOrigin("trace button clicked", UserActionOrigin.Insights)

    val settingsState = SettingsState.getInstance()
    val jaegerBaseUrl = settingsState.jaegerUrl?.trim()?.trimEnd('/')
    val jaegerUrl: String
    val embedPart = "&uiEmbed=v0"

    jaegerUrl =
        "${jaegerBaseUrl}/trace/${traceId1.lowercase()}...${traceId2.lowercase()}?cohort=${traceId1.lowercase()}&cohort=${traceId2.lowercase()}${embedPart}"

    when (settingsState.jaegerLinkMode) {

        LinkMode.Internal -> {

            val caption = "Comparing: A sample $traceName1 trace with a $traceName2 trace"

            val htmlContent = JaegerEmbeddedHtmlTemplate.JAEGER_EMBEDDED_HTML_TEMPLATE
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
            val traceSample1 = TraceSample(traceName1, traceId1)
            val traceSample2 = TraceSample(traceName2, traceId2)
            val traces = listOf(traceSample1, traceSample2)
            JaegerUIService.getInstance(project).openEmbeddedJaeger(traces, traceName1, null, true)
        }
    }
}


fun isJaegerButtonEnabled(): Boolean {
    val settingsState = SettingsState.getInstance()
    return settingsState.jaegerLinkMode == LinkMode.Embedded ||
            (!settingsState.jaegerUrl.isNullOrBlank() && CommonUtils.isWelFormedUrl(settingsState.jaegerUrl))
}

fun getJaegerUrl(): String? {
    val settingsState = SettingsState.getInstance()
    val jaegerBaseUrl = settingsState.jaegerUrl?.trim()?.trimEnd('/')
    val jaegerQueryUrl = SettingsState.getInstance().jaegerQueryUrl

    return when (settingsState.jaegerLinkMode) {

        LinkMode.Internal -> {
            jaegerBaseUrl
        }
        LinkMode.External -> {
            jaegerBaseUrl
        }
        LinkMode.Embedded -> {
            jaegerQueryUrl
        }
    }
}


private class JaegerEmbeddedHtmlTemplate {

    companion object {
        val JAEGER_EMBEDDED_HTML_TEMPLATE: String = this::class.java.getResourceAsStream("/templates/Jaeger-embedded-template.html")?.let {
            CharStreams.toString(InputStreamReader(it))
        }.toString()
    }
}