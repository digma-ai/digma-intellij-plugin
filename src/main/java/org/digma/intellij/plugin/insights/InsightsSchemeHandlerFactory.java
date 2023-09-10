package org.digma.intellij.plugin.insights;

import com.intellij.openapi.project.Project;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefSchemeHandlerFactory;
import org.cef.handler.CefResourceHandler;
import org.cef.network.CefRequest;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;

class InsightsSchemeHandlerFactory implements CefSchemeHandlerFactory {
    private final Project project;

    public InsightsSchemeHandlerFactory(Project project) {
        this.project = project;
    }

    @Override
    public CefResourceHandler create(CefBrowser browser, CefFrame frame, String schemeName, CefRequest request) {
        var url = getUrl(request);
        if (url != null) {
            var host = url.getHost();
            var file = url.getFile();
            if (InsightsServiceImpl.DOMAIN_NAME.equals(host) &&
                    InsightsServiceImpl.SCHEMA_NAME.equals(schemeName)) {
                var resourceName = InsightsServiceImpl.RESOURCE_FOLDER_NAME + file;
                var resource = getClass().getResource(resourceName);
                if (resource != null) {
                    return new InsightsResourceHandler(project, resourceName);
                } else {
                    return new InsightsResourceHandler(project, InsightsServiceImpl.RESOURCE_FOLDER_NAME + "/index.html");
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
