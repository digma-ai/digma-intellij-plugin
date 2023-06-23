package org.digma.intellij.plugin.jcef.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.ui.jcef.JBCefBrowser;
import org.digma.intellij.plugin.ui.settings.Theme;
import org.jetbrains.annotations.NotNull;

import static org.digma.intellij.plugin.jcef.common.JCefMessagesUtils.GLOBAL_SET_UI_CODE_FONT;
import static org.digma.intellij.plugin.jcef.common.JCefMessagesUtils.GLOBAL_SET_UI_MAIN_FONT;
import static org.digma.intellij.plugin.jcef.common.JCefMessagesUtils.GLOBAL_SET_UI_THEME;
import static org.digma.intellij.plugin.jcef.common.JCefMessagesUtils.REQUEST_MESSAGE_TYPE;

public class JCefBrowserUtil {


    private JCefBrowserUtil() {}


    public static void sendRequestToChangeUiTheme(@NotNull Theme uiTheme, JBCefBrowser jbCefBrowser) {
        String requestMessage = resultToString(
                new UIThemeRequest(
                        REQUEST_MESSAGE_TYPE,
                        GLOBAL_SET_UI_THEME,
                        new UiThemePayload(uiTheme.getThemeName())
                ));
        postJSMessage(requestMessage, jbCefBrowser);
    }

    public static void sendRequestToChangeFont(String font, JBCefBrowser jbCefBrowser) {
        String requestMessage = resultToString(
                new UIFontRequest(
                        REQUEST_MESSAGE_TYPE,
                        GLOBAL_SET_UI_MAIN_FONT,
                        new UiFontPayload(font)
                ));
        postJSMessage(requestMessage, jbCefBrowser);
    }


    public static void sendRequestToChangeCodeFont(String font, JBCefBrowser jbCefBrowser) {
        String requestMessage = JCefBrowserUtil.resultToString(
                new UICodeFontRequest(
                        REQUEST_MESSAGE_TYPE,
                        GLOBAL_SET_UI_CODE_FONT,
                        new UiCodeFontPayload(font)
                ));
        JCefBrowserUtil.postJSMessage(requestMessage, jbCefBrowser);
    }



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
