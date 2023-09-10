package org.digma.intellij.plugin.ui.jcef

import com.intellij.openapi.Disposable
import com.intellij.ui.jcef.JBCefBrowser
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils
import org.digma.intellij.plugin.settings.SettingsState
import org.digma.intellij.plugin.ui.jcef.model.JaegerUrlChangedPayload
import org.digma.intellij.plugin.ui.jcef.model.JaegerUrlChangedRequest
import org.digma.intellij.plugin.ui.list.insights.isJaegerButtonEnabled

class JaegerButtonStateListener {

    fun start(parentDisposable: Disposable, jCefComponent: JCefComponent) {
        SettingsState.getInstance().addChangeListener({
            changeTraceButtonVisibility(jCefComponent.jbCefBrowser)
        }, parentDisposable)
    }


    private fun changeTraceButtonVisibility(jbCefBrowser: JBCefBrowser) {

        val jaegerButtonRequest = JaegerUrlChangedRequest(
            JCefMessagesUtils.REQUEST_MESSAGE_TYPE,
            JCefMessagesUtils.GLOBAL_SET_IS_JAEGER_ENABLED,
            JaegerUrlChangedPayload(isJaegerButtonEnabled())
        )

        serializeAndExecuteWindowPostMessageJavaScript(jbCefBrowser.cefBrowser, jaegerButtonRequest)
    }

}