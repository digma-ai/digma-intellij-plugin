package org.digma.intellij.plugin.ui.errors.model

import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants

class SetErrorsDismissMessage(val payload: ErrorActionResult) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "ERRORS/SET_DISMISS_ERROR_RESULT"
}