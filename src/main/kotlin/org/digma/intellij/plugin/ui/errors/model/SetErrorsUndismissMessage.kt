package org.digma.intellij.plugin.ui.errors.model

import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants

class SetErrorsUndismissMessage(val payload: ErrorActionResult) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "ERRORS/SET_UNDISMISS_ERROR_RESULT"
}