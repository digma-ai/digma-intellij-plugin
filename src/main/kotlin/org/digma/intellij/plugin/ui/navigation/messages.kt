package org.digma.intellij.plugin.ui.navigation

import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.navigation.View
import org.digma.intellij.plugin.ui.jcef.serializeAndExecuteWindowPostMessageJavaScript
import org.digma.intellij.plugin.ui.navigation.model.CodeContextMessage
import org.digma.intellij.plugin.ui.navigation.model.SetViewMessage
import org.digma.intellij.plugin.ui.navigation.model.SetViewMessagePayload


fun sendCurrentViewsState(cefBrowser: CefBrowser, views: List<View>) {
    serializeAndExecuteWindowPostMessageJavaScript(
        cefBrowser,
        SetViewMessage(SetViewMessagePayload(views))
    )
}


fun sendCodeContext(cefBrowser: CefBrowser, codeContextMessage: CodeContextMessage) {
    serializeAndExecuteWindowPostMessageJavaScript(
        cefBrowser, codeContextMessage
    )
}
