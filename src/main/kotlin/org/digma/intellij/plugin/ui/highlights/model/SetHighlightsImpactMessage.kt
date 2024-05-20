package org.digma.intellij.plugin.ui.highlights.model

import com.fasterxml.jackson.annotation.JsonRawValue
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants

data class SetHighlightsImpactMessage(@JsonRawValue val payload: String?) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "MAIN/SET_HIGHLIGHTS_IMPACT_DATA"
}
