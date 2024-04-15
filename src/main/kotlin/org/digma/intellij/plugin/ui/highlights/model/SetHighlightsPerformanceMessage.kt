package org.digma.intellij.plugin.ui.highlights.model

import com.fasterxml.jackson.annotation.JsonRawValue
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants

data class SetHighlightsPerformanceMessage(@JsonRawValue val payload: String?) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "MAIN/SET_HIGHLIGHTS_PERFORMANCE_DATA"
}
