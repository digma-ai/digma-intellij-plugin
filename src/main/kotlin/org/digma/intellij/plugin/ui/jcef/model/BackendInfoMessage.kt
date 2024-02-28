package org.digma.intellij.plugin.ui.jcef.model

import org.digma.intellij.plugin.model.rest.AboutResult
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants

data class BackendInfoMessage(val payload: AboutResult) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = JCEFGlobalConstants.GLOBAL_SET_BACKEND_INFO
}

