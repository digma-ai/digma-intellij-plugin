package org.digma.intellij.plugin.documentation;

import org.cef.browser.CefBrowser;
import org.cef.handler.CefResourceHandler;
import org.digma.intellij.plugin.ui.jcef.BaseSchemeHandlerFactory;
import org.jetbrains.annotations.NotNull;

public class DocumentationSchemeHandlerFactory extends BaseSchemeHandlerFactory {

    @NotNull
    @Override
    public CefResourceHandler createResourceHandler(@NotNull CefBrowser browser, @NotNull String resourcePath) {
        return new DocumentationResourceHandler(browser, resourcePath);
    }

    @NotNull
    @Override
    public String getSchema() {
        return DocumentationConstants.DOCUMENTATION_SCHEMA;
    }

    @NotNull
    @Override
    public String getDomain() {
        return DocumentationConstants.DOCUMENTATION_DOMAIN_NAME;
    }

}
