package org.digma.intellij.plugin.ui.highlights.model

import org.digma.intellij.plugin.model.rest.highlights.HighlightsPerformanceResponse
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants

data class SetHighlightsPerformanceMessage(val payload: List<HighlightsPerformanceResponse>?) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "HIGHLIGHTS/SET_PERFORMANCE"
}
