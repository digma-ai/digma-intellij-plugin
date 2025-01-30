package org.digma.intellij.plugin.jaegerui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefResourceHandler;
import org.digma.intellij.plugin.ui.jcef.BaseSchemeHandlerFactory;
import org.jetbrains.annotations.*;

import java.net.URL;

import static org.digma.intellij.plugin.jaegerui.JaegerUIConstants.JAEGER_UI_SCHEMA;
import static org.digma.intellij.plugin.jaegerui.JaegerProxyResourceHandler.getJaegerQueryUrlOrNull;

public class JaegerUiSchemeHandlerFactory extends BaseSchemeHandlerFactory {

    private static final Logger LOGGER = Logger.getInstance(JaegerUiSchemeHandlerFactory.class);

    @Nullable
    @Override
    public CefResourceHandler createProxyHandler(@NotNull Project project, @NotNull URL url) {
        //this method checks only for jaeger calls from jaeger ui app only and not from other jcef apps.
        //it is backward support for jaeger ui that still sends requests to /api/ instead of /jaeger/api/
        var jaegerQueryUrl = getJaegerQueryUrlOrNull();
        if (jaegerQueryUrl != null &&
                JaegerProxyResourceHandler.isJaegerQueryCallFromJaegerUI(url)) {
            return new JaegerProxyResourceHandler(jaegerQueryUrl);
        }
        return super.createProxyHandler(project, url);
    }

    @NotNull
    @Override
    public CefResourceHandler createResourceHandler(@NotNull CefBrowser browser, @NotNull String resourcePath) {
        return new JaegerUiResourceHandler(browser, resourcePath);
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
}
