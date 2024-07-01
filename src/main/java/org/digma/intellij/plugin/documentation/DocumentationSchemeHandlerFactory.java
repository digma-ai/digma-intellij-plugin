package org.digma.intellij.plugin.documentation;

import org.cef.browser.CefBrowser;
import org.cef.handler.CefResourceHandler;
import org.digma.intellij.plugin.ui.jcef.BaseSchemeHandlerFactory;
import org.jetbrains.annotations.NotNull;

public class DocumentationSchemeHandlerFactory extends BaseSchemeHandlerFactory {


    @NotNull
    @Override
    public CefResourceHandler createResourceHandler(@NotNull String resourceName, boolean resourceExists, @NotNull CefBrowser browser) {
        if (resourceExists) {
            return new DocumentationResourceHandler(browser, resourceName);
        } else {
            return new DocumentationResourceHandler(browser, DocumentationConstants.DOCUMENTATION_RESOURCE_FOLDER_NAME + "/index.html");
        }
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

    @NotNull
    @Override
    public String getResourceFolderName() {
        return DocumentationConstants.DOCUMENTATION_RESOURCE_FOLDER_NAME;
    }
}
