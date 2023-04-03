package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.impl.HTMLEditorProvider
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.settings.LinkMode
import org.digma.intellij.plugin.settings.SettingsState
import org.digma.intellij.plugin.ui.list.ListItemActionButton
import org.digma.intellij.plugin.ui.model.TraceSample
import javax.swing.JButton

// if cannot create the button then would return null
fun buildButtonToJaeger(
        project: Project, linkCaption: String, spanName: String, traceSamples: List<TraceSample?>
): JButton? {

    val settingsState = SettingsState.getInstance()

    val jaegerBaseUrl = settingsState.jaegerUrl?.trim()?.trimEnd('/')
    if (jaegerBaseUrl.isNullOrBlank() || traceSamples.isNullOrEmpty()) {
        return null
    }
    val filtered = traceSamples.filter { x -> x != null && x.hasTraceId() }
    if (filtered.isNullOrEmpty()) {
        return null
    }

    val caption: String
    val jaegerUrl: String
    val embedPart = "&uiEmbed=v0"

    val trace1 = filtered[0]!!.traceId?.lowercase()
    if (filtered.size == 1) {
        caption = "A sample ${filtered[0]!!.traceName} trace"
        jaegerUrl = "${jaegerBaseUrl}/trace/${trace1}?cohort=${trace1}${embedPart}"
    } else {
        // assuming it has (at least) size of 2
        val trace2 = filtered[1]!!.traceId?.lowercase()
        caption = "Comparing: A sample ${filtered[0]!!.traceName} trace with a ${filtered[1]!!.traceName} trace"
        jaegerUrl = "${jaegerBaseUrl}/trace/${trace1}...${trace2}?cohort=${trace1}&cohort=${trace2}${embedPart}"
    }

    val htmlContent = SpanPanels.JAEGER_EMBEDDED_HTML_TEMPLATE
            .replace("__JAEGER_EMBEDDED_URL__", jaegerUrl)
            .replace("__CAPTION__", caption)

    val editorTitle = "Jaeger sample traces of Span $spanName"

    val button = ListItemActionButton(linkCaption)
    if (settingsState.jaegerLinkMode == LinkMode.Internal) {
        button.addActionListener {
            HTMLEditorProvider.openEditor(project, editorTitle, htmlContent)
            ActivityMonitor.getInstance(project).registerInsightButtonClicked("show-in-jaeger")
        }
    } else {
        // handle LinkMode.External
        button.addActionListener {
            BrowserUtil.browse(jaegerUrl, project)
            ActivityMonitor.getInstance(project).registerInsightButtonClicked("show-in-jaeger")
        }
    }

    return button
}

fun openJaegerFromRecentActivity(
        project: Project,
        traceId: String,
        spanName: String
) {
    val settingsState = SettingsState.getInstance()

    val jaegerBaseUrl = settingsState.jaegerUrl?.trim()?.trimEnd('/')
    if (jaegerBaseUrl.isNullOrBlank() || traceId.isNullOrBlank()) {
        return
    }

    val jaegerUrl: String
    val embedPart = "&uiEmbed=v0"

    val traceIdLowerCased = traceId.lowercase()
    jaegerUrl = "${jaegerBaseUrl}/trace/${traceIdLowerCased}?cohort=${traceIdLowerCased}${embedPart}"

    val caption = "A sample $spanName trace"
    val htmlContent = SpanPanels.JAEGER_EMBEDDED_HTML_TEMPLATE
            .replace("__JAEGER_EMBEDDED_URL__", jaegerUrl)
            .replace("__CAPTION__", caption)
    val editorTitle = "Jaeger sample traces of Span $spanName"

    if (settingsState.jaegerLinkMode == LinkMode.Internal) {
        // open Jaeger inside IDE
        ApplicationManager.getApplication().invokeLater {
            HTMLEditorProvider.openEditor(project, editorTitle, htmlContent)
        }
    } else {
        // handle LinkMode.External - open Jaeger in Web browser
        ApplicationManager.getApplication().invokeLater {
            BrowserUtil.browse(jaegerUrl, project)
        }
    }
}

fun isJaegerUrlPresentInUserSettings(): Boolean {
    val settingsState = SettingsState.getInstance()
    val jaegerBaseUrl = settingsState.jaegerUrl?.trim()?.trimEnd('/')
    return !jaegerBaseUrl.isNullOrBlank()
}