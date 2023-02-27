package org.digma.intellij.plugin.toolwindow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.ui.jcef.JBCefBrowser;

public class JBCefBrowserUtil {

    public static void postJSMessage(String jsonMessage, JBCefBrowser jbCefBrowser) {
        jbCefBrowser.getCefBrowser().executeJavaScript(
                "window.postMessage(" + jsonMessage + ");",
                jbCefBrowser.getCefBrowser().getURL(),
                0
        );
    }

    public static String resultToString(Object result) {
        try {
            return new ObjectMapper().writeValueAsString(result);
        } catch (Exception e) {
            return "Error parsing object " + e.getMessage();
        }
    }
}
