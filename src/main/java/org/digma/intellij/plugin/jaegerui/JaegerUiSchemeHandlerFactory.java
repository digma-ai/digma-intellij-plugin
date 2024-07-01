package org.digma.intellij.plugin.jaegerui;

import org.cef.browser.CefBrowser;
import org.cef.handler.CefResourceHandler;
import org.digma.intellij.plugin.ui.jcef.BaseSchemeHandlerFactory;
import org.jetbrains.annotations.NotNull;

import static org.digma.intellij.plugin.jaegerui.JaegerUIConstants.JAEGER_UI_SCHEMA;

public class JaegerUiSchemeHandlerFactory extends BaseSchemeHandlerFactory {


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
}
