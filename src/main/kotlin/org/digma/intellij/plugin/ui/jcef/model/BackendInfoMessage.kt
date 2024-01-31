package org.digma.intellij.plugin.ui.jcef.model

import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils
import org.digma.intellij.plugin.model.rest.AboutResult

data class BackendInfoMessage(val payload: AboutResult) {
    val type = JCefMessagesUtils.REQUEST_MESSAGE_TYPE
    val action = JCefMessagesUtils.GLOBAL_SET_BACKEND_INFO
}

