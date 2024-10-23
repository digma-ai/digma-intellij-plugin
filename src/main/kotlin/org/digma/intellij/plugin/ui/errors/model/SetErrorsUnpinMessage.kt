package org.digma.intellij.plugin.ui.errors.model

import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants

class SetErrorsUnpinMessage(val payload: ErrorActionResult) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = "ERRORS/SET_UNPIN_ERROR_RESULT"
}