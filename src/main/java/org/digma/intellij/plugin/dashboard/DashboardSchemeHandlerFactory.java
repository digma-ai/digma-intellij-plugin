package org.digma.intellij.plugin.dashboard;

import org.cef.browser.CefBrowser;
import org.cef.handler.CefResourceHandler;
import org.digma.intellij.plugin.ui.jcef.BaseSchemeHandlerFactory;
import org.jetbrains.annotations.NotNull;


public class DashboardSchemeHandlerFactory extends BaseSchemeHandlerFactory {

    @NotNull
    @Override
    public CefResourceHandler createResourceHandler(@NotNull CefBrowser browser, @NotNull String resourcePath) {
        return new DashboardResourceHandler(browser, resourcePath);
    }

    @NotNull
    @Override
    public String getSchema() {
        return DashboardConstants.DASHBOARD_SCHEMA;
    }

    @NotNull
    @Override
    public String getDomain() {
        return DashboardConstants.DASHBOARD_DOMAIN_NAME;
    }
}