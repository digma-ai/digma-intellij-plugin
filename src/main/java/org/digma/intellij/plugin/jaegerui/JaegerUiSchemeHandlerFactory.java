package org.digma.intellij.plugin.jaegerui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefResourceHandler;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.settings.SettingsState;
import org.digma.intellij.plugin.ui.jcef.BaseSchemeHandlerFactory;
import org.jetbrains.annotations.*;

import java.net.*;

import static org.digma.intellij.plugin.jaegerui.JaegerUIConstants.JAEGER_UI_SCHEMA;

public class JaegerUiSchemeHandlerFactory extends BaseSchemeHandlerFactory {

    private static final Logger LOGGER = Logger.getInstance(JaegerUiSchemeHandlerFactory.class);

    @Nullable
    @Override
    public CefResourceHandler createProxyHandler(@NotNull Project project, @NotNull URL url) {
        var jaegerQueryUrl = GetJaegerQueryUrlOrNull();
        if (jaegerQueryUrl != null &&
                JaegerUiProxyResourceHandler.isJaegerQueryCall(url)) {
            return new JaegerUiProxyResourceHandler(jaegerQueryUrl);
        }
        return null;
    }

    @NotNull
    @Override
    public CefResourceHandler createResourceHandler(@NotNull String resourceName, boolean resourceExists, @NotNull CefBrowser browser) {
        if (resourceExists) {
            return new JaegerUiResourceHandler(browser, resourceName);
        } else {
            return new JaegerUiResourceHandler(browser, JaegerUIConstants.JAEGER_UI_RESOURCE_FOLDER_NAME + "/index.html");
        }
    }

    @NotNull
    @Override
    public String getSchema() {
        return JAEGER_UI_SCHEMA;
    }

    @NotNull
    @Override
    public String getDomain() {
        return JaegerUIConstants.JAEGER_UI_DOMAIN_NAME;
    }

    @NotNull
    @Override
    public String getResourceFolderName() {
        return JaegerUIConstants.JAEGER_UI_RESOURCE_FOLDER_NAME;
    }

    private static URL GetJaegerQueryUrlOrNull(){
        var urlStr = SettingsState.getInstance().getJaegerQueryUrl();
        if(urlStr == null)
            return null;

        try {
            return new URL(urlStr);
        } catch (MalformedURLException e) {
            Log.warnWithException(LOGGER, e, "JaegerQueryUrl parsing failed");
        }
        return null;
    }
}
