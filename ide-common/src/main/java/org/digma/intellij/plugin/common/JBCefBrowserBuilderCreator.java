package org.digma.intellij.plugin.common;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.jcef.JBCefBrowserBuilder;

public class JBCefBrowserBuilderCreator {
    public static JBCefBrowserBuilder create()
    {
        JBCefBrowserBuilder jbCefBrowserBuilder = new JBCefBrowserBuilder();
        if (SystemInfo.isLinux) {
            jbCefBrowserBuilder.setOffScreenRendering(true); // setting it to false may cause focus issues on some linux os
        }

        var enableDevTools = Boolean.getBoolean("org.digma.plugin.enable.devtools");

        jbCefBrowserBuilder.setEnableOpenDevToolsMenuItem(enableDevTools);
        return jbCefBrowserBuilder;
    }
}
