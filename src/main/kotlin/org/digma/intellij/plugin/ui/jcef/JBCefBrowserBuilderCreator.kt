package org.digma.intellij.plugin.ui.jcef

import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.jcef.JBCefBrowserBuilder

object JBCefBrowserBuilderCreator {

    @JvmStatic
    fun create(): JBCefBrowserBuilder {

        val jbCefBrowserBuilder = JBCefBrowserBuilder()
        if (SystemInfo.isLinux && isOffScreenRenderingModeEnabled()) {
            jbCefBrowserBuilder.setOffScreenRendering(true) // setting it to false may cause focus issues on some linux os
        }

        val enableDevTools = java.lang.Boolean.getBoolean("org.digma.plugin.enable.devtools")
        jbCefBrowserBuilder.setEnableOpenDevToolsMenuItem(enableDevTools)

        return jbCefBrowserBuilder
    }


    private fun isOffScreenRenderingModeEnabled(): Boolean {
        return java.lang.Boolean.getBoolean("ide.browser.jcef.osr.enabled")
    }
}
