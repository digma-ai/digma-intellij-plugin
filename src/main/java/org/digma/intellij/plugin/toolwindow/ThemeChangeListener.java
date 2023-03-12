package org.digma.intellij.plugin.toolwindow;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.jcef.JBCefBrowser;
import org.apache.commons.lang3.StringUtils;
import org.digma.intellij.plugin.log.Log;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static org.digma.intellij.plugin.toolwindow.ToolWindowUtil.RECENT_ACTIVITY_SET_UI_THEME;
import static org.digma.intellij.plugin.toolwindow.ToolWindowUtil.REQUEST_MESSAGE_TYPE;

public class ThemeChangeListener implements PropertyChangeListener {
    private static final Logger LOGGER = Logger.getInstance(ThemeChangeListener.class);
    private JBCefBrowser jbCefBrowser;

    public ThemeChangeListener(JBCefBrowser jbCefBrowser) {
        this.jbCefBrowser = jbCefBrowser;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if ("lookAndFeel".equals(evt.getPropertyName())) {
            // The UI theme has been changed
            changeUiThemeForRecentActivity();
        }
    }

    private void changeUiThemeForRecentActivity() {
        String theme = ThemeUtil.getCurrentThemeName();
        if (StringUtils.isNotEmpty(theme)) {
            changeUiThemeForRecentActivityTab(jbCefBrowser, theme);
            Log.log(LOGGER::debug, "UI theme changed to " + theme);
        } else {
            Log.log(LOGGER::debug, "UI theme was not changed because theme was null or empty.");
        }
    }

    private void changeUiThemeForRecentActivityTab(JBCefBrowser jbCefBrowser, String uiTheme) {
        String requestMessage = JBCefBrowserUtil.resultToString(new UIThemeRequest(
                REQUEST_MESSAGE_TYPE,
                RECENT_ACTIVITY_SET_UI_THEME,
                new UiThemePayload(uiTheme)
        ));
        JBCefBrowserUtil.postJSMessage(requestMessage, jbCefBrowser);
    }
}