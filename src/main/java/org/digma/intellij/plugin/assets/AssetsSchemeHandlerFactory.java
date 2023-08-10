package org.digma.intellij.plugin.assets;

import com.intellij.openapi.project.Project;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefSchemeHandlerFactory;
import org.cef.handler.CefResourceHandler;
import org.cef.network.CefRequest;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;

class AssetsSchemeHandlerFactory implements CefSchemeHandlerFactory {
    private final Project project;

    public AssetsSchemeHandlerFactory(Project project) {
        this.project = project;
    }

    @Override
    public CefResourceHandler create(CefBrowser browser, CefFrame frame, String schemeName, CefRequest request) {
        var url = getUrl(request);
        if (url != null) {
            var host = url.getHost();
            var file = url.getFile();
            if (AssetsService.DOMAIN_NAME.equals(host) &&
                    AssetsService.SCHEMA_NAME.equals(schemeName)) {
                var resourceName = AssetsService.RESOURCE_FOLDER_NAME + file;
                var resource = getClass().getResource(resourceName);
                if (resource != null) {
                    return new AssetsResourceHandler(project, resourceName);
                } else {
                    return new AssetsResourceHandler(project, AssetsService.RESOURCE_FOLDER_NAME + "/index.html");
                }
            }
        }
        return null;
    }

    @Nullable
    private URL getUrl(CefRequest request) {
        try {
            return new URL(request.getURL());
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
