package org.digma.intellij.plugin.ui.errors.model

import com.fasterxml.jackson.annotation.JsonRawValue
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants

class SetErrorTimeSeriesDataMessage(@JsonRawValue val payload: String?) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "ERRORS/SET_ERROR_TIME_SERIES_DATA"
}